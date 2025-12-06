package io.github.rothes.esu.bukkit.module.core

import io.github.rothes.esu.bukkit.module.CoreModule
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.core.util.extension.readUuid
import io.github.rothes.esu.core.util.extension.writeUuid
import kotlinx.io.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

object CoreController {

    private val lastAttackTime = ConcurrentHashMap<Player, Long>()
    private val lastMoveTime = ConcurrentHashMap<Player, Long>()

    fun onEnable() {
        Listeners.register()
        HotDataHandler.loadData()
        val now = System.currentTimeMillis()
        for (p in Bukkit.getOnlinePlayers()) {
            lastMoveTime.putIfAbsent(p, now)
        }
    }

    fun onDisable() {
        Listeners.unregister()
        HotDataHandler.saveData()
        lastAttackTime.clear()
        lastMoveTime.clear()
    }

    private object HotDataHandler {

        private const val DATA_VERSION: Byte = 1

        private val hotData
            get() = CoreModule.moduleFolder.resolve("hotData.tmp")

        fun loadData() {
            try {
                if (!plugin.enabledHot || !hotData.exists()) return
                hotData.inputStream().asSource().buffered().use { buf ->
                    require(buf.readByte() == DATA_VERSION) { "Different hot data version" }
                    buf.readTimeMap(lastAttackTime)
                    buf.readTimeMap(lastMoveTime)
                }
            } catch (e: Throwable) {
                plugin.err("[CoreModule] Failed to load hotData", e)
            }
        }

        fun saveData() {
            try {
                if (!plugin.disabledHot) return
                hotData.outputStream(StandardOpenOption.CREATE).use { stream ->
                    Buffer().apply {
                        writeByte(DATA_VERSION)
                        writeTimeMap(lastAttackTime)
                        writeTimeMap(lastMoveTime)
                    }.copyTo(stream)
                }
                hotData.toFile().deleteOnExit()
            } catch (e: Throwable) {
                plugin.err("[CoreModule] Failed to save hotData", e)
            }
        }

        private fun Sink.writeTimeMap(map: Map<Player, Long>) {
            writeInt(map.size)
            for ((player, time) in map) {
                writeUuid(player.uniqueId)
                writeLong(time)
            }
        }
        private fun Source.readTimeMap(map: MutableMap<Player, Long>) {
            val size = readInt()
            repeat(size) {
                val uuid = readUuid()
                val time = readLong()
                Bukkit.getPlayer(uuid)?.let { player ->
                    map.putIfAbsent(player, time)
                }
            }
        }
    }

    object RunningProvider: Provider {

        override val isEnabled: Boolean = true

        override fun lastAttackTime(player: Player): Long {
            return lastAttackTime[player] ?: 0
        }

        override fun lastMoveTime(player: Player): Long {
            return lastMoveTime[player] ?: System.currentTimeMillis()
        }

    }

    private object Listeners: Listener {

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            val p = event.player
            lastMoveTime[p] = System.currentTimeMillis()
        }

        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            val p = event.player
            lastMoveTime[p] = System.currentTimeMillis()
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            val p = event.player
            lastAttackTime.remove(p)
            lastMoveTime.remove(p)
        }

        @EventHandler
        fun onDamage(event: EntityDamageByEntityEvent) {
            val damager = event.damager as? Player ?: return
            lastAttackTime[damager] = System.currentTimeMillis()
        }

    }

}