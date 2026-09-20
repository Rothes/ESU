package io.github.rothes.esu.bukkit.module.anticheat.transaction

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing
import io.github.rothes.esu.bukkit.module.anticheat.PrePacketEventManager
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object PingPong : CommonFeature<FeatureToggle.DefaultTrue, Unit>() {

    private val playerMap = ConcurrentHashMap<Player, PlayerData>()
    private val pongRegistered = AtomicBoolean(false)

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents()
    }

    override fun onEnable() {
        BukkitListener.register()
        PacketEvents.getAPI().eventManager.registerListener(PingListener)
        val players = Bukkit.getOnlinePlayers()
        for (player in players) {
            playerMap[player] = PlayerData(true)
        }
        if (players.isNotEmpty()) {
            // We have players, wait 30s for a buffer, delay check
            Scheduler.global(30 * 20) {
                if (enabled) {
                    PrePacketEventManager.eventManager.registerListener(PongListener)
                    pongRegistered.set(true)
                }
            }
        } else {
            PrePacketEventManager.eventManager.registerListener(PongListener)
            pongRegistered.set(true)
        }
    }

    override fun onDisable() {
        super.onDisable()
        BukkitListener.unregister()
        pongRegistered.set(false)
        PrePacketEventManager.eventManager.unregisterListeners(PongListener)
        PacketEvents.getAPI().eventManager.unregisterListener(PingListener)
        playerMap.clear()
    }

    private data class PlayerData(
        var firstCheck: Boolean = false,
        val pendingRequests: Queue<Int> = ArrayDeque(),
    )

    private object BukkitListener : Listener {

        @EventHandler
        fun onJoin(event: PlayerJoinEvent) {
            playerMap[event.player] = PlayerData(!pongRegistered.get())
        }

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            playerMap.remove(event.player)
        }
    }

    private object PingListener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketSend(event: PacketSendEvent) {
            if (event.packetType == PacketType.Play.Server.PING) {
                val data = playerMap[event.getPlayer()] ?: return
                data.pendingRequests.add(WrapperPlayServerPing(event).id)
            }
        }
    }

    private object PongListener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketReceive(event: PacketReceiveEvent) {
            if (event.packetType == PacketType.Play.Client.PONG) {
                val data = playerMap[event.getPlayer()] ?: return
                val id = WrapperPlayClientPong(event).id
                if (data.firstCheck) {
                    while (true) {
                        val first = data.pendingRequests.peek()
                        if (first == null || first == id) break
                        data.pendingRequests.remove()
                    }
                    data.firstCheck = false
                }
                if (data.pendingRequests.isEmpty() || data.pendingRequests.poll() != id) {
                    event.isCancelled = true
                }
            }
        }
    }

}