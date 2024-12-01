package io.github.rothes.esu.bukkit.module.chatantispam.user

import cc.carm.lib.easysql.api.SQLQuery
import cc.carm.lib.easysql.api.SQLTable
import cc.carm.lib.easysql.api.action.PreparedSQLUpdateAction
import cc.carm.lib.easysql.api.enums.IndexType
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.addr
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.config
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.DataSerializer.decode
import io.github.rothes.esu.bukkit.util.DataSerializer.encode
import io.github.rothes.esu.core.storage.StorageManager
import org.bukkit.Bukkit
import java.sql.ResultSet
import java.sql.Timestamp

object CasDataManager {

    val TABLE = SQLTable.of("chat_spam_data") {
        it.addColumn("user INT UNSIGNED NOT NULL")
            .addColumn("ip VARCHAR(45) NOT NULL")
            .addColumn("lastAccess DATETIME NOT NULL")
            .addColumn("data BLOB NOT NULL")
            .setIndex("user", IndexType.UNIQUE_KEY)
            .setIndex("ip", IndexType.UNIQUE_KEY)
//                .addForeignKey("user", "users", "id")
    }
    val cacheById = hashMapOf<Int, SpamData>()
    val cacheByIp = hashMapOf<String, SpamData>()

    operator fun get(user: PlayerUser): SpamData {
        cacheByIp[user.addr]?.let {
            return it
        }
        val created = SpamData()
        cacheById[user.dbId] = created
        cacheByIp[user.addr] = created
        return created
    }

    init {
        TABLE.create(StorageManager.sqlManager)
        TABLE.createDelete(StorageManager.sqlManager)
            .addTimeCondition("lastAccess", -1, System.currentTimeMillis() - config.userDataExpiresAfter)
            .build().execute()
        Bukkit.getOnlinePlayers().forEach { loadSpamData(it.user) }
    }

    fun loadSpamData(user: PlayerUser, async: Boolean = true) {
        val dbId = user.dbId
        val addr = user.addr

        val query = TABLE.createQuery(StorageManager.sqlManager)
            .selectColumns("user", "ip", "lastAccess", "data")
            .addCondition(" ip = ? OR user = ? ")
            .orderBy("lastAccess", false).setLimit(1)
            .build().setParams(addr, dbId)

        val handler: (SQLQuery) -> Unit = { res ->
            var spamData = latest(cacheById[dbId], cacheByIp[addr])
            val resultSet = res.resultSet
            if (resultSet.next()) {
                val id = resultSet.getInt("user")
                val ip = resultSet.getString("ip")
                spamData = latest(spamData, resultSet.spamData)!!

                cacheById[id] = spamData
                cacheByIp[ip] = spamData
                Bukkit.getOnlinePlayers().filter { it.address!!.hostString == ip }.forEach { cacheById[it.user.dbId] = spamData }
            }
            spamData?.let {
                cacheById[dbId] = spamData
                cacheByIp[addr] = spamData
                Bukkit.getOnlinePlayers().filter { it.address!!.hostString == addr }.forEach { cacheById[it.user.dbId] = spamData }
            }
        }
        if (async) {
            query.executeAsync(handler)
        } else {
            query.execute().use {
                handler(it)
            }
        }
    }

    private fun latest(o1: SpamData?, o2: SpamData?): SpamData? {
        if (o1 == null)
            return o2
        if (o2 == null)
            return null
        return if (o1.lastAccess > o2.lastAccess) o1 else o2
    }

    private fun buildSaveSpamData(user: PlayerUser): PreparedSQLUpdateAction<Int>? {
        val spamData = cacheById[user.dbId] ?: return null
        val data = spamData.encode()
        return TABLE.createReplace(StorageManager.sqlManager)
            .setColumnNames("user", "ip", "lastAccess", "data")
            .setParams(user.dbId, user.addr, Timestamp(kotlin.math.max(spamData.lastAccess, spamData.muteUntil)), data)
    }

    fun saveSpamDataAsync(user: PlayerUser) {
        buildSaveSpamData(user)?.executeAsync()
    }

    fun saveSpamData(user: PlayerUser) {
        buildSaveSpamData(user)?.execute()
    }

    fun deleteAsync(key: Any?) {
        val dbId = key as? Int ?: return
        TABLE.createDelete(StorageManager.sqlManager)
            .addCondition("user", dbId)
            .build().executeAsync()
    }

    private val ResultSet.spamData: SpamData
        get() = getBytes("data").decode<SpamData>().also {
            it.lastAccess = getTimestamp("lastAccess").time
        }

}