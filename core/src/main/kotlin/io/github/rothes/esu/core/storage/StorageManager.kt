package io.github.rothes.esu.core.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.rothes.esu.core.EsuCore
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
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*

object StorageManager {

    private val datasource = HikariDataSource(HikariConfig().apply {
        poolName = "ESU-HikariPool"
        driverClassName = EsuConfig.get().database.driverClassName
        jdbcUrl         = EsuConfig.get().database.url
        username        = EsuConfig.get().database.username
        password        = EsuConfig.get().database.password
    })
    val database: Database = try {
        Database.connect(datasource).also {
            if (EsuConfig.get().database.url.startsWith("jdbc:h2:")) {
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
        val uuid = uuid("uuid").uniqueIndex("uk_uuid")
        val name = varchar("name", 16, "utf8mb3_general_ci").nullable().uniqueIndex("uk_name")
        val language = varchar("language", 12, "utf8mb3_general_ci").nullable()
        val colorScheme = varchar("color_scheme", 32, "utf8mb3_general_ci").nullable()

        override val primaryKey: PrimaryKey = PrimaryKey(dbId)
    }

    init {
        transaction(database) {
            SchemaUtils.create(MetaTable)
            // <editor-fold desc="TableUpgrader">
            UsersTable.upgrader({
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
            }, {
                exec("ALTER TABLE `$tableName` DROP INDEX `users_uuid_unique`")
                exec("ALTER TABLE `$tableName` DROP INDEX `users_name_unique`")
                exec("ALTER TABLE `$tableName` ADD UNIQUE INDEX `uk_uuid` (uuid)")
                exec("ALTER TABLE `$tableName` ADD UNIQUE INDEX `uk_name` (name)")
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

    fun getUuid(userId: Int): UUID? {
        return with(UsersTable) {
            transaction(database) {
                select(uuid).where(dbId eq userId).singleOrNull()?.get(uuid)
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

    fun getUserDataByName(where: String): UserData? {
        return with(UsersTable) {
            transaction(database) {
                select(uuid, dbId, language, colorScheme).where(name eq where).singleOrNull()?.let {
                    UserData(it[dbId], it[uuid], it[language], it[colorScheme])
                }
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
        transaction(database) {
            try {
                doUpdateUser(user)
            } catch (e: ExposedSQLException) {
                if (e.cause is SQLIntegrityConstraintViolationException) {
                    EsuCore.instance.err("Failed to update user ${user.dbId} data ${user.nameUnsafe} (${user.uuid}): " + e.message)
                    user.nameUnsafe?.let { dbName ->
                        val get = getUserDataByName(dbName)
                        if (get != null) {
                            EsuCore.instance.warn("Current in db user ${get.dbId}: ${user.nameUnsafe} (${get.uuid})")
                            if (get.dbId != user.dbId) {
                                with(UsersTable) {
                                    update({ dbId eq get.dbId }) {
                                        it[name] = null
                                    }
                                }
                                doUpdateUser(user)
                                EsuCore.instance.warn("Re-attached name ${user.nameUnsafe} to user ${user.dbId}")
                            }
                        }
                    }
                } else {
                    throw e
                }
            }
        }
    }

    fun updateUserAsync(user: User) {
        coroutineScope.launch {
            updateUserNow(user)
        }
    }

    private fun doUpdateUser(user: User) {
        with(UsersTable) {
            update({ dbId eq user.dbId }) {
                it[name] = user.nameUnsafe
                it[language] = user.languageUnsafe
                it[colorScheme] = user.colorSchemeUnsafe
            }
        }
    }

    fun shutdown() {
        datasource.close()
        TransactionManager.closeAndUnregister(database)
    }

    data class UserData(
        val dbId: Int,
        val uuid: UUID,
        val language: String?,
        val colorScheme: String?,
    )

    fun Table.upgrader(vararg upgradeHandlers: () -> Unit) {
        TableUpgrader(this, *upgradeHandlers)
    }

    class TableUpgrader(
        table: Table,
        vararg upgradeHandlers: () -> Unit,
    ) {
        init {
            val tableName = table.tableName
            val tbKey = "tbv_$tableName"

            val version = upgradeHandlers.size + 1

            var currentVer = 1
            if (SchemaUtils.listTables().map { it.substringAfter('.') }.any { it.equals(tableName, true)}) {
                currentVer = MetaTable.select(MetaTable.value).where(MetaTable.key eq tbKey)
                    .singleOrNull()?.let { it[MetaTable.value].toInt() } ?: 1
                while (currentVer < version) {
                    EsuCore.instance.info("Upgrading schema of db table $tableName to $currentVer")
                    try {
                        upgradeHandlers[currentVer - 1]()
                    } catch (e: Exception) {
                        EsuCore.instance.err("Failed to upgrade schema of db table $tableName to $currentVer, ignoring: ${e.message ?: e.toString()}")
                        break
                    }
                    currentVer++
                }
            } else {
                currentVer = version
            }
            MetaTable.upsert {
                it[key] = tbKey
                it[value] = currentVer.toString()
            }
        }
    }

}