package io.github.rothes.esu.bukkit.module.chatantispam.user

import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.addr
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.config
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.DataSerializer.deserialize
import io.github.rothes.esu.bukkit.util.DataSerializer.serialize
import io.github.rothes.esu.core.storage.StorageManager
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bukkit.Bukkit
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.between
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.replace
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json

object CasDataManager {

    object ChatSpam: Table("chat_spam_data") {
        val user = integer("user").uniqueIndex()
        val ip = varchar("ip", 45).uniqueIndex()
        val lastAccess = datetime("last_access")
        val data = json<SpamData>("data", { it.serialize() }, { it.deserialize() })

        init {
            transaction {
                SchemaUtils.create(ChatSpam)
                ChatSpam.deleteWhere {
                    lastAccess.between((-1L).localDateTime, (System.currentTimeMillis() - config.userDataExpiresAfter).localDateTime)
                }
            }
        }
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
        Bukkit.getOnlinePlayers().forEach { loadSpamData(it.user) }
    }

    fun loadSpamData(where: PlayerUser, async: Boolean = true) {
        val dbId = where.dbId
        val addr = where.addr

        fun func() {
            var spamData = latest(cacheById[dbId], cacheByIp[addr]) // Current cached
            with(ChatSpam) {
                transaction {
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
        with(ChatSpam) {
            transaction {
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

    fun deleteAsync(key: Any?) {
        StorageManager.coroutineScope.launch {
            when (key) {
                is Int    -> {
                    transaction {
                        ChatSpam.deleteWhere { user eq key }
                    }
                }
                is String -> {
                    transaction {
                        ChatSpam.deleteWhere { ip   eq key }
                    }
                }
            }
        }
    }

    private val Long.localDateTime
        get() = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
}