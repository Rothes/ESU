package io.github.rothes.esu.bukkit.module.core.persistence

import com.github.luben.zstd.Zstd
import io.github.rothes.esu.bukkit.module.CoreModule
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.coroutine.IOScope
import io.github.rothes.esu.core.storage.StorageManager.database
import io.github.rothes.esu.core.storage.userId
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ConversionUtils.localDateTime
import io.github.rothes.esu.core.util.DataSerializer.decode
import io.github.rothes.esu.core.util.DataSerializer.encode
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

object CorePersistentStorage {

    object CoreTable: Table("core_persistent_storage") {
        val user = userId()
        val server = varchar("server", 32)
        val lastUpdate = datetime("last_update").defaultExpression(CurrentDateTime)
        val data = blob("data")
        val dataVersion = enumeration<DataVersion>("data_version")

        init {
            uniqueIndex("uk_user__server", user, server)

            transaction(database) {
                SchemaUtils.create(CoreTable)
            }
        }
    }

    enum class DataVersion(
        val serializer: (PersistentData) -> ByteArray,
        val deserializer: (ByteArray) -> PersistentData
    ) {
        PLAIN({ it.encode() },                     { it.decode() }),
        ZSTD ({ Zstd.compress(it.encode()) },{ Zstd.decompress(it).decode() }),
    }

    fun saveUserData(u: PlayerUser, d: PersistentData, v: DataVersion = DataVersion.ZSTD) {
        if (!u.logonBefore) return
        IOScope.launch {
            transaction(database) {
                CoreTable.upsert {
                    it[user] = u.dbId
                    it[server] = CoreModule.config.persistentStorage.serverName
                    it[lastUpdate] = System.currentTimeMillis().localDateTime
                    it[data] = ExposedBlob(v.serializer(d))
                    it[dataVersion] = v
                }
            }
        }
    }

    fun loadUserData(u: User): PersistentData? {
        return transaction(database) {
            with(CoreTable) {
                CoreTable.select(data, dataVersion).where {
                    (user eq u.dbId) and (server eq CoreModule.config.persistentStorage.serverName)
                }.singleOrNull()?.let {
                    it[dataVersion].deserializer(it[data].bytes)
                }
            }
        }
    }

}