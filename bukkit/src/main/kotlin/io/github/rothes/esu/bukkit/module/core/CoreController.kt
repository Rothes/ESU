package io.github.rothes.esu.bukkit.module.core

import io.github.rothes.esu.bukkit.module.CoreModule
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.util.extension.readUuid
import io.github.rothes.esu.core.util.extension.writeUuid
import kotlinx.io.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.jetbrains.annotations.ApiStatus
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

object CoreController {

    fun onEnable() {
        Listeners.register()
        HotDataHandler.loadData()
        val now = System.currentTimeMillis()
        for (p in Bukkit.getOnlinePlayers()) {
            RunningProviders.moveTime.map.putIfAbsent(p, now)
            RunningProviders.posMoveTime.map.putIfAbsent(p, now)
            RunningProviders.attackTime.map.putIfAbsent(p, now)
        }
    }

    fun onDisable() {
        Listeners.unregister()
        HotDataHandler.saveData()
        RunningProviders.moveTime.map.clear()
        RunningProviders.posMoveTime.map.clear()
        RunningProviders.attackTime.map.clear()
        RunningProviders.genericActiveTime.map.clear()
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
                    buf.readTimeMap(RunningProviders.attackTime.map)
                    buf.readTimeMap(RunningProviders.moveTime.map, RunningProviders.posMoveTime.map)
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
                        writeTimeMap(RunningProviders.attackTime.map)
                        writeTimeMap(RunningProviders.moveTime.map)
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
        private fun Source.readTimeMap(vararg maps: MutableMap<Player, Long>) {
            val size = readInt()
            repeat(size) {
                val uuid = readUuid()
                val time = readLong()
                Bukkit.getPlayer(uuid)?.let { player ->
                    for (map in maps) {
                        map.putIfAbsent(player, time)
                    }
                }
            }
        }
    }

    object RunningProviders: Providers {

        override val isEnabled: Boolean = true

        override val attackTime = MapPlayerTimeProvider()
        override val posMoveTime = MapPlayerTimeProvider()
        override val moveTime = MapPlayerTimeProvider()

        override val genericActiveTime = MapPlayerTimeProvider().also {
            val listener = object : PlayerTimeProvider.ChangeListener {
                override fun onTimeChanged(player: Player, oldTime: Long, newTime: Long) {
                    it[player] = newTime
                }
            }
            attackTime.registerListener(listener)
            posMoveTime.registerListener(listener)
            moveTime.registerListener(listener)
        }

    }

    private object Listeners: Listener {

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            val p = event.player
            val now = System.currentTimeMillis()
            RunningProviders.moveTime[p] = now
            if (event.from.world != event.to.world || event.from.distanceSquared(event.to) >= 1f / 128) {
                RunningProviders.posMoveTime[p] = now
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        fun onPlayerJoin(event: PlayerJoinEvent) {
            val p = event.player
            val now = System.currentTimeMillis()
            RunningProviders.moveTime[p] = now
            RunningProviders.posMoveTime[p] = now
            RunningProviders.attackTime[p] = now
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onPlayerQuit(event: PlayerQuitEvent) {
            val p = event.player
            RunningProviders.attackTime.unload(p)
            RunningProviders.posMoveTime.unload(p)
            RunningProviders.moveTime.unload(p)
            RunningProviders.genericActiveTime.unload(p)
        }

        @EventHandler
        fun onDamage(event: EntityDamageByEntityEvent) {
            val damager = event.damager as? Player ?: return
            RunningProviders.attackTime[damager] = System.currentTimeMillis()
        }

    }

    class MapPlayerTimeProvider: PlayerTimeProvider {

        @get:ApiStatus.Internal
        val map = ConcurrentHashMap<Player, Long>()
        private val listeners = CopyOnWriteArrayList<PlayerTimeProvider.ChangeListener>()

        override fun get(player: Player): Long = map[player] ?: 0

        override fun set(player: Player, time: Long) {
            val old = map.put(player, time) ?: 0
            for (listener in listeners) {
                try {
                    listener.onTimeChanged(player, old, time)
                } catch (e: Throwable) {
                    plugin.err("An provider exception occurred:", e)
                }
            }
        }

        fun unload(player: Player) {
            map.remove(player)
        }

        override fun registerListener(listener: PlayerTimeProvider.ChangeListener) {
            listeners.add(listener)
        }

        override fun unregisterListener(listener: PlayerTimeProvider.ChangeListener) {
            listeners.remove(listener)
        }

    }

}