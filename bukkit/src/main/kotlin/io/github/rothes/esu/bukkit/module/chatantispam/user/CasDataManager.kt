package io.github.rothes.esu.bukkit.module.chatantispam.user

import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.addr
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.config
import io.github.rothes.esu.bukkit.module.chatantispam.user.CasDataManager.ChatSpamTable.tableName
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.DataSerializer.deserialize
import io.github.rothes.esu.bukkit.util.DataSerializer.serialize
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.storage.StorageManager.TableUpgrader
import io.github.rothes.esu.core.storage.StorageManager.database
import io.github.rothes.esu.core.util.ConversionUtils.localDateTime
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.between
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.replace
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json

object CasDataManager {

    object ChatSpamTable: Table("chat_spam_data") {
        val user = integer("user").references(StorageManager.UsersTable.dbId, ReferenceOption.CASCADE, ReferenceOption.NO_ACTION).uniqueIndex()
        val ip = varchar("ip", 45, collate = "ascii_general_ci").uniqueIndex()
        val lastAccess = datetime("last_access")
        val data = json<SpamData>("data", { it.serialize() }, { it.deserialize() })
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
        transaction(database) {
            // <editor-fold desc="TableUpgrader">
            TableUpgrader(ChatSpamTable, 2, {
                println("Upgrading ChatSpamDataTable")
                fun alter(column: String, type: String) {
                    exec("ALTER TABLE `$tableName` MODIFY COLUMN `$column` $type")
                }
                exec("ALTER TABLE `$tableName` CHANGE COLUMN `lastAccess` `last_access` DATETIME(6) NOT NULL")
                alter("user", "INT(11) NOT NULL")
                alter("ip", "VARCHAR(45) NOT NULL COLLATE ascii_general_ci")
                alter("data", "TEXT NOT NULL COLLATE utf8mb4_bin")
                exec("ALTER TABLE `$tableName` ADD CONSTRAINT `fk_chat_spam_data_user__id` FOREIGN KEY (`user`) REFERENCES `users` (`id`) ON UPDATE CASCADE ON DELETE NO ACTION")
                exec("ALTER TABLE `$tableName` ADD CONSTRAINT `data` CHECK (json_valid(`data`))")
            })
            // </editor-fold>
            SchemaUtils.create(ChatSpamTable)
            ChatSpamTable.deleteWhere {
                lastAccess.between((-1L).localDateTime, (System.currentTimeMillis() - config.userDataExpiresAfter).localDateTime)
            }
        }
        Bukkit.getOnlinePlayers().forEach { loadSpamData(it.user) }
    }

    fun loadSpamData(where: PlayerUser, async: Boolean = true) {
        val dbId = where.dbId
        val addr = where.addr

        fun func() {
            var spamData = latest(cacheById[dbId], cacheByIp[addr]) // Current cached
            with(ChatSpamTable) {
                transaction(database) {
                    selectAll().where { (ip eq addr) or (user eq dbId) }.orderBy(lastAccess, SortOrder.DESC)
                        .limit(1).singleOrNull()?.let { row ->
                            spamData = latest(spamData, row[data])!!.also { sd ->
                                val ip = row[ip]
                                cacheById[row[user]] = sd
                                cacheByIp[ip] = sd
                                Bukkit.getOnlinePlayers().filter { it.address!!.hostString == ip }.forEach { cacheById[it.user.dbId] = sd }
                            }
                        }
                }
            }
            spamData?.let { sd ->
                cacheById[dbId] = sd
                cacheByIp[addr] = sd
                Bukkit.getOnlinePlayers().filter { it.address!!.hostString == addr }.forEach { cacheById[it.user.dbId] = sd }
            }
        }

        if (async) {
            StorageManager.coroutineScope.launch {
                func()
            }
        } else {
            func()
        }
    }

    private fun latest(o1: SpamData?, o2: SpamData?): SpamData? {
        if (o1 == null)
            return o2
        if (o2 == null)
            return null
        return if (o1.lastAccess > o2.lastAccess) o1 else o2
    }

    fun saveSpamData(where: PlayerUser) {
        val spamData = cacheById[where.dbId] ?: return
        with(ChatSpamTable) {
            transaction(database) {
                replace {
                    it[user] = where.dbId
                    it[ip] = where.addr
                    it[lastAccess] = kotlin.math.max(spamData.lastAccess, spamData.muteUntil).localDateTime
                    it[data] = spamData
                }
            }
        }
    }

    fun saveSpamDataAsync(where: PlayerUser) {
        StorageManager.coroutineScope.launch {
            saveSpamData(where)
        }
    }

    fun deleteAsync(keys: List<Any?>) {
        val byId = mutableListOf<Int>()
        val byIp = mutableListOf<String>()
        for (any in keys) {
            when (any) {
                is Int    -> byId.add(any)
                is String -> byIp.add(any)
                else      -> error("Unknown key type ${any?.javaClass?.name} ($any)")
            }
        }
        StorageManager.coroutineScope.launch {
            transaction(database) {
                ChatSpamTable.deleteWhere {
                    buildList {
                        if (byId.isNotEmpty()) add(user inList byId)
                        if (byIp.isNotEmpty()) add(ip   inList byIp)
                    }.compoundOr()
                }
            }
        }
    }

    fun deleteAsync(key: Any?) {
        val where = when (key) {
            is Int    -> ChatSpamTable.user eq key
            is String -> ChatSpamTable.ip   eq key
            else      -> error("Unknown key type ${key?.javaClass?.name} ($key)")
        }
        StorageManager.coroutineScope.launch {
            transaction(database) {
                ChatSpamTable.deleteWhere { where }
            }
        }
    }

}