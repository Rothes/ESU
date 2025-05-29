package io.github.rothes.esu.core.storage

import cc.carm.lib.easysql.EasySQL
import cc.carm.lib.easysql.hikari.HikariConfig
import cc.carm.lib.easysql.hikari.HikariDataSource
import cc.carm.lib.easysql.manager.SQLManagerImpl
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.storage.StorageManager.UsersTable.colorScheme
import io.github.rothes.esu.core.storage.StorageManager.UsersTable.dbId
import io.github.rothes.esu.core.storage.StorageManager.UsersTable.language
import io.github.rothes.esu.core.storage.StorageManager.UsersTable.name
import io.github.rothes.esu.core.storage.StorageManager.UsersTable.tableName
import io.github.rothes.esu.core.storage.StorageManager.UsersTable.uuid
import io.github.rothes.esu.core.user.ConsoleConst
import io.github.rothes.esu.core.user.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

object StorageManager {

    private var legacyManager = false
    @Deprecated("Migrating to Exposed")
    val sqlManager: SQLManagerImpl by lazy {
        try {
            legacyManager = true
            EasySQL.createManager(EsuConfig.get().database.jdbcDriver, EsuConfig.get().database.jdbcUrl, EsuConfig.get().database.username, EsuConfig.get().database.password)!!.apply {
                if (EsuConfig.get().database.jdbcUrl.startsWith("jdbc:h2:")) {
                    executeSQL("SET MODE=MYSQL")
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to connect to database", e)
        }
    }

    private val datasource = HikariDataSource(HikariConfig().apply {
        poolName = "ESU-HikariPool"
        driverClassName = EsuConfig.get().database.jdbcDriver
        jdbcUrl         = EsuConfig.get().database.jdbcUrl
        username        = EsuConfig.get().database.username
        password        = EsuConfig.get().database.password
    })
    val database: Database = try {
        Database.connect(datasource).also {
            if (EsuConfig.get().database.jdbcUrl.startsWith("jdbc:h2:")) {
                transaction(it) {
                    exec("SET MODE=MYSQL")
                }
            }
        }
    } catch (e: Exception) {
        throw RuntimeException("Failed to connect to database", e)
    }
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    object MetaTable : Table("metadata") {
        val key = varchar("key", 32)
        val value = text("value")

        override val primaryKey = PrimaryKey(key)
    }

    object UsersTable : Table("users") {
        val dbId = integer("id").autoIncrement()
        val uuid = uuid("uuid").uniqueIndex()
        val name = varchar("name", 16, "utf8mb3_general_ci").nullable().uniqueIndex()
        val language = varchar("language", 12, "utf8mb3_general_ci").nullable()
        val colorScheme = varchar("color_scheme", 32, "utf8mb3_general_ci").nullable()

        override val primaryKey: PrimaryKey = PrimaryKey(dbId)
    }

    init {
        transaction(database) {
            SchemaUtils.create(MetaTable)
            // <editor-fold desc="TableUpgrader">
            TableUpgrader(UsersTable, 2, {
                println("Upgrading UsersTable")
                exec("ALTER TABLE `$tableName` RENAME TO `${tableName}_old`")
                val oldTable = object : Table("users_old") {
                    val dbId = integer("id").autoIncrement()
                    val uuid = varchar("uuid", 36).uniqueIndex()
                    val name = varchar("name", 16).nullable().uniqueIndex()
                    val language = varchar("language", 12).nullable()
                    val colorScheme = varchar("color_scheme", 32).nullable()
                }
                SchemaUtils.create(UsersTable)
                UsersTable.batchInsert(oldTable.selectAll()) { data ->
                    this[dbId] = data[oldTable.dbId]
                    this[uuid] = UUID.fromString(data[oldTable.uuid])
                    this[name] = data[oldTable.name]
                    this[language] = data[oldTable.language]
                    this[colorScheme] = data[oldTable.colorScheme]
                }
                SchemaUtils.drop(oldTable)
            })
            // </editor-fold>
            SchemaUtils.create(UsersTable)
            UsersTable.insertIgnore {
                it[dbId] = ConsoleConst.DATABASE_ID
                it[uuid] = ConsoleConst.UUID
                it[name] = ConsoleConst.NAME
            }
        }
    }

    fun getUserData(where: UUID): UserData {
        return with(UsersTable) {
            transaction(database) {
                select(dbId, language, colorScheme).where(uuid eq where).singleOrNull()?.let {
                    UserData(it[dbId], where, it[language], it[colorScheme])
                } ?: UserData(insert { it[uuid] = where }[dbId], where, null, null)
            }
        }
    }

    fun getConsoleUserData(): UserData {
        return with(UsersTable) {
            transaction(database) {
                select(language, colorScheme).where(dbId eq ConsoleConst.DATABASE_ID).single().let {
                    UserData(ConsoleConst.DATABASE_ID, ConsoleConst.UUID, it[language], it[colorScheme])
                }
            }
        }
    }

    fun updateUserNow(user: User) {
        with(UsersTable) {
            transaction(database) {
                update({ dbId eq user.dbId }) {
                    it[name] = user.nameUnsafe
                    it[language] = user.languageUnsafe
                    it[colorScheme] = user.colorSchemeUnsafe
                }
            }
        }
    }

    fun updateUserAsync(user: User) {
        coroutineScope.launch {
            updateUserNow(user)
        }
    }

    fun shutdown() {
        if (legacyManager) (sqlManager.dataSource as HikariDataSource).close()
        datasource.close()
        TransactionManager.closeAndUnregister(database)
    }

    data class UserData(
        val dbId: Int,
        val uuid: UUID,
        val language: String?,
        val colorScheme: String?,
    )

    class TableUpgrader(
        table: Table,
        version: Int,
        vararg upgradeHandlers: () -> Unit,
    ) {
        init {
            val tableName = table.tableName
            val tbKey = "tbv_$tableName"

            if (SchemaUtils.listTables().map { it.substringAfter('.') }.contains(tableName)) {
                val schemaVer = MetaTable.select(MetaTable.value).where(MetaTable.key eq tbKey)
                    .singleOrNull()?.let { it[MetaTable.value].toInt() } ?: 1
                for (i in schemaVer until version) {
                    upgradeHandlers[i - 1]()
                }
            }
            MetaTable.upsert {
                it[key] = tbKey
                it[value] = version.toString()
            }
        }
    }

}