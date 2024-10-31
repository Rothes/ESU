package io.github.rothes.esu.core.storage

import cc.carm.lib.easysql.EasySQL
import cc.carm.lib.easysql.api.SQLTable
import io.github.rothes.esu.EsuConfig
import io.github.rothes.esu.core.user.ConsoleConst
import io.github.rothes.esu.core.user.User
import java.util.UUID

object StorageManager {

    private val userTable = SQLTable.of("users") {
        it.addAutoIncrementColumn("id")
            .addColumn("uuid", "VARCHAR(36) NOT NULL UNIQUE KEY")
            .addColumn("name", "VARCHAR(16) UNIQUE KEY")
    }

    val sqlManager = try {
        EasySQL.createManager(EsuConfig.get().database.jdbcDriver, EsuConfig.get().database.jdbcUrl, EsuConfig.get().database.username, EsuConfig.get().database.password)!!
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

    fun getUserId(uuid: UUID): Int {
        userTable.createQuery(sqlManager)
            .selectColumns("id")
            .addCondition("uuid", uuid)
            .build().execute().use { sqlQuery ->
                val resultSet = sqlQuery.resultSet
                if (resultSet.next()) {
                    return resultSet.getInt("id")
                } else {
                    return userTable.createInsert(sqlManager)
                        .setColumnNames("uuid").setParams(uuid)
                        .returnGeneratedKey().execute()
                }
            }
    }

    fun updateUserNameAsync(user: User) {
        val id = user.dbId
        val nameUnsafe = user.nameUnsafe
        if (nameUnsafe != null)
            userTable.createReplace(sqlManager)
                .setColumnNames("id", "uuid", "name").setParams(id, user.uuid, nameUnsafe)
                .executeAsync()
    }

}