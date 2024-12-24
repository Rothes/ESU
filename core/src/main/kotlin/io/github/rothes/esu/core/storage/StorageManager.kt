package io.github.rothes.esu.core.storage

import cc.carm.lib.easysql.EasySQL
import cc.carm.lib.easysql.api.SQLTable
import cc.carm.lib.easysql.api.enums.IndexType
import cc.carm.lib.easysql.manager.SQLManagerImpl
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.user.ConsoleConst
import io.github.rothes.esu.core.user.User
import java.util.*

object StorageManager {

    private val userTable = SQLTable.of("users") {
        it.addAutoIncrementColumn("id")
            .addColumn("uuid", "VARCHAR(36) NOT NULL")
            .addColumn("name", "VARCHAR(16)")
            .addColumn("language", "VARCHAR(12)")
            .addColumn("color_scheme", "VARCHAR(32)")
            .setIndex("uuid", IndexType.UNIQUE_KEY)
            .setIndex("name", IndexType.UNIQUE_KEY)
    }

    val sqlManager: SQLManagerImpl = try {
        EasySQL.createManager(EsuConfig.get().database.jdbcDriver, EsuConfig.get().database.jdbcUrl, EsuConfig.get().database.username, EsuConfig.get().database.password)!!.apply {
            if (EsuConfig.get().database.jdbcUrl.startsWith("jdbc:h2:")) {
                executeSQL("SET MODE=MYSQL")
            }
        }
    } catch (e: Exception) {
        throw RuntimeException("Failed to connect to database", e)
    }

    init {
        userTable.create(sqlManager)
        userTable.createInsert(sqlManager)
            .setColumnNames("id", "uuid", "name")
            .setParams(ConsoleConst.DATABASE_ID, ConsoleConst.UUID, ConsoleConst.NAME)
            .execute()
    }

    fun getUserData(uuid: UUID): UserData {
        userTable.createQuery(sqlManager)
            .selectColumns("id", "language", "color_scheme")
            .addCondition("uuid", uuid)
            .build().execute().use { sqlQuery ->
                val resultSet = sqlQuery.resultSet
                return if (resultSet.next()) {
                    UserData(resultSet.getInt("id"),
                        uuid, resultSet.getString("language"), resultSet.getString("color_scheme"))
                } else {
                    val id = userTable.createInsert(sqlManager)
                        .setColumnNames("uuid")
                        .setParams(uuid)
                        .returnGeneratedKey().execute()
                    UserData(id, uuid, null, null)
                }
            }
    }

    fun getConsoleUserData(): UserData {
        userTable.createQuery(sqlManager)
            .selectColumns("language", "color_scheme")
            .addCondition("id", ConsoleConst.DATABASE_ID)
            .build().execute().use { sqlQuery ->
                val resultSet = sqlQuery.resultSet
                resultSet.next()
                return UserData(ConsoleConst.DATABASE_ID, ConsoleConst.UUID,
                        resultSet.getString("language"), resultSet.getString("color_scheme"))
            }
    }

    fun updateUserNameAsync(user: User) {
        val id = user.dbId
        val nameUnsafe = user.nameUnsafe
        if (nameUnsafe != null)
            userTable.createReplace(sqlManager)
                .setColumnNames("id", "uuid", "name", "language", "color_scheme")
                .setParams(id, user.uuid, nameUnsafe, user.languageUnsafe, user.colorSchemeUnsafe)
                .executeAsync()
    }

    data class UserData(
        val dbId: Int,
        val uuid: UUID,
        val language: String?,
        val colorScheme: String?,
    )

}