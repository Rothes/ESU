package io.github.rothes.esu.bukkit.module.chatantispam.user

import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.addr
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.config
import io.github.rothes.esu.bukkit.module.chatantispam.user.CasDataManager.ChatSpamTable.lastAccess
import io.github.rothes.esu.bukkit.module.chatantispam.user.CasDataManager.ChatSpamTable.tableName
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.storage.StorageManager.database
import io.github.rothes.esu.core.storage.StorageManager.upgrader
import io.github.rothes.esu.core.util.ConversionUtils.epochMilli
import io.github.rothes.esu.core.util.ConversionUtils.localDateTime
import io.github.rothes.esu.core.util.DataSerializer.deserialize
import io.github.rothes.esu.core.util.DataSerializer.serialize
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object CasDataManager {

    object ChatSpamTable: Table("chat_spam_data") {
        val user = integer("user").references(StorageManager.UsersTable.dbId, ReferenceOption.CASCADE, ReferenceOption.CASCADE, "fk_chat_spam_data__user__id").uniqueIndex("uk_user")
        val ip = varchar("ip", 45, collate = "ascii_general_ci").uniqueIndex("uk_ip")
        val lastAccess = datetime("last_access")
        val data = json<SpamData>("data", { it.serialize() }, { it.deserialize() })
    }
    private val cacheById = hashMapOf<Int, SpamDataHolder>()
    private val cacheByIp = hashMapOf<String, SpamDataHolder>()
    private val lock = ReentrantReadWriteLock()

    operator fun get(user: PlayerUser): SpamData {
        return getHolder(user).spamData
    }

    fun getHolder(user: PlayerUser): SpamDataHolder {
        lock.read {
            cacheByIp[user.addr]?.let {
                return it
            }
        }
        lock.write {
            val created = SpamDataHolder(SpamData())
            cacheById[user.dbId] = created
            cacheByIp[user.addr] = created
            return created
        }
    }

    fun getCache(user: PlayerUser): SpamData? {
        return lock.read { cacheByIp[user.addr]?.spamData }
    }

    fun purgeCache(deleteDb: Boolean) {
        val time = System.currentTimeMillis()
        val toDel = mutableListOf<Any>()
        val handler = { map: MutableMap<out Any, SpamDataHolder> ->
            val iterator = map.iterator()
            for ((key, value) in iterator) {
                if (time - value.spamData.lastAccess > config.userDataExpiresAfter.toMillis()) {
                    if (deleteDb)
                        toDel.add(key)
                    iterator.remove()
                }
            }
        }
        lock.write {
            handler(cacheById)
            handler(cacheByIp)
        }
        if (toDel.isNotEmpty()) {
            deleteExpiredAsync(keys = toDel)
        }
    }

    init {
        transaction(database) {
            // <editor-fold desc="TableUpgrader">
            ChatSpamTable.upgrader({
                fun alter(column: String, type: String) {
                    exec("ALTER TABLE `$tableName` MODIFY COLUMN `$column` $type")
                }
                exec("ALTER TABLE `$tableName` CHANGE COLUMN `lastAccess` `last_access` DATETIME(6) NOT NULL")
                alter("user", "INT(11) NOT NULL")
                alter("ip", "VARCHAR(45) NOT NULL COLLATE ascii_general_ci")
                alter("data", "TEXT NOT NULL COLLATE utf8mb4_bin")
                exec("ALTER TABLE `$tableName` ADD CONSTRAINT `fk_chat_spam_data_user__id` FOREIGN KEY (`user`) REFERENCES `users` (`id`) ON UPDATE CASCADE ON DELETE NO ACTION")
                exec("ALTER TABLE `$tableName` ADD CONSTRAINT `data` CHECK (json_valid(`data`))")
            }, {
                exec("ALTER TABLE `$tableName` DROP FOREIGN KEY `fk_chat_spam_data_user__id`")
                exec("ALTER TABLE `$tableName` ADD CONSTRAINT `fk_chat_spam_data_user__id` FOREIGN KEY (`user`) REFERENCES `users` (`id`) ON UPDATE NO ACTION ON DELETE CASCADE")
            }, {
                exec("ALTER TABLE `$tableName` DROP FOREIGN KEY `fk_chat_spam_data_user__id`")
                exec("ALTER TABLE `$tableName` ADD CONSTRAINT `fk_chat_spam_data_user__id` FOREIGN KEY (`user`) REFERENCES `users` (`id`) ON UPDATE CASCADE ON DELETE CASCADE")
            }, {
                exec("ALTER TABLE `$tableName` DROP FOREIGN KEY `fk_chat_spam_data_user__id`")
                exec("ALTER TABLE `$tableName` DROP INDEX `user`")
                exec("ALTER TABLE `$tableName` DROP INDEX `ip`")
                exec("ALTER TABLE `$tableName` ADD UNIQUE INDEX `uk_user` (`user`)")
                exec("ALTER TABLE `$tableName` ADD UNIQUE INDEX `uk_ip` (ip)")
                exec("ALTER TABLE `$tableName` ADD CONSTRAINT `fk_chat_spam_data__user__id` FOREIGN KEY (`user`) REFERENCES `users` (`id`) ON UPDATE CASCADE ON DELETE CASCADE")
            }, {
                try {
                    val column = if (database.dialect is H2Dialect) """"user"""" else "`user`"
                    // Create index names are wrong, but upgrading was ok...
                    exec("""ALTER TABLE `$tableName` DROP INDEX `fk_chat_spam_data__user__id`""")
                    exec("""ALTER TABLE `$tableName` ADD CONSTRAINT `uk_user` UNIQUE ($column)""")
                    exec("""ALTER TABLE `$tableName` DROP FOREIGN KEY `fk_user__id`""")
                    exec("""ALTER TABLE `$tableName` ADD CONSTRAINT `fk_chat_spam_data__user__id` FOREIGN KEY ($column) REFERENCES `users` (`id`) ON UPDATE CASCADE ON DELETE CASCADE""")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
            // </editor-fold>
            SchemaUtils.create(ChatSpamTable)
            ChatSpamTable.deleteWhere { expiredOp }
        }
        Bukkit.getOnlinePlayers().forEach { loadSpamData(it.user) }
    }

    fun loadSpamData(where: PlayerUser) {
        val dbId = where.dbId
        val addr = where.addr

        StorageManager.coroutineScope.launch {
            with(ChatSpamTable) {
                transaction(database) {
                    selectAll().where { (ip eq addr) or (user eq dbId) }.orderBy(lastAccess, SortOrder.DESC)
                        .limit(1).singleOrNull()?.let { row ->
                            val cached = lock.read { latest(cacheById[dbId], cacheByIp[addr]) }
                            val data = row[data].also { it.lastAccess = row[lastAccess].epochMilli }
                            latest(cached, data).also { holder ->
                                lock.write {
                                    cacheById[dbId] = holder
                                    cacheByIp[addr] = holder
                                }
                            }
                        }
                }
            }
        }
    }

    private fun latest(o1: SpamDataHolder?, o2: SpamDataHolder?): SpamDataHolder? {
        if (o1 == null)
            return o2
        if (o2 == null)
            return null
        return if (o1.spamData.lastAccess > o2.spamData.lastAccess) {
            o2.spamData = o1.spamData
            o1
        } else {
            o1.spamData = o2.spamData
            o2
        }
    }

    private fun latest(holder: SpamDataHolder?, data: SpamData): SpamDataHolder {
        if (holder == null)
            return SpamDataHolder(data)
        if (holder.spamData.lastAccess < data.lastAccess) {
            holder.spamData = data
        }
        return holder
    }

    fun saveSpamDataNow(where: PlayerUser) {
        val holder = lock.read { latest(cacheById[where.dbId], cacheByIp[where.addr]) } ?: return
        val spamData = holder.spamData
        val lastAccessValue = kotlin.math.max(spamData.lastAccess, spamData.muteUntil).localDateTime
        with(ChatSpamTable) {
            transaction(database) {
                val inserted = insertIgnore {
                    it[user] = where.dbId
                    it[ip] = where.addr
                    it[lastAccess] = lastAccessValue
                    it[data] = spamData
                }.insertedCount
                if (inserted == 0) {
                    update({ ((user eq where.dbId) or (ip eq where.addr)) and (lastAccess less lastAccessValue) }) {
                        it[lastAccess] = lastAccessValue
                        it[data] = spamData
                    }
                }
            }
        }
    }

    fun saveSpamDataAsync(where: PlayerUser) {
        StorageManager.coroutineScope.launch {
            saveSpamDataNow(where)
        }
    }

    fun deleteExpiredAsync(keys: List<Any?>) {
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
                    }.compoundOr() and expiredOp
                }
            }
        }
    }

    fun deleteExpiredAsync(key: Any?) {
        val where = when (key) {
            is Int    -> ChatSpamTable.user eq key
            is String -> ChatSpamTable.ip   eq key
            else      -> error("Unknown key type ${key?.javaClass?.name} ($key)")
        }
        StorageManager.coroutineScope.launch {
            transaction(database) {
                ChatSpamTable.deleteWhere { where and expiredOp }
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

    private val expiredOp
        get() = lastAccess.between((-1L).localDateTime, (System.currentTimeMillis() - config.userDataExpiresAfter.toMillis()).localDateTime)

}