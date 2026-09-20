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
import io.github.rothes.esu.bukkit.util.collection.fastutil.ints.IntArrayQueue
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object PingPong : CommonFeature<FeatureToggle.DefaultTrue, Unit>() {

    private val playerMap = ConcurrentHashMap<Player, PlayerData>()

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
        PrePacketEventManager.eventManager.registerListener(PongListener)
    }

    override fun onDisable() {
        super.onDisable()
        BukkitListener.unregister()
        PrePacketEventManager.eventManager.unregisterListeners(PongListener)
        PacketEvents.getAPI().eventManager.unregisterListener(PingListener)
        playerMap.clear()
    }

    private data class PlayerData(
        var waitingForSync: Boolean = false,
        val pendingRequests: IntArrayQueue = IntArrayQueue(),
    )

    private object BukkitListener : Listener {

        @EventHandler
        fun onJoin(event: PlayerJoinEvent) {
            playerMap[event.player] = PlayerData()
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
                val pending = data.pendingRequests
                if (pending.size() >= 30 * 20) { // Hold 600 ticks for GrimAC
                    pending.dequeueInt()
                }
                pending.enqueue(WrapperPlayServerPing(event).id)
            }
        }
    }

    private object PongListener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketReceive(event: PacketReceiveEvent) {
            if (event.packetType == PacketType.Play.Client.PONG) {
                val data = playerMap[event.getPlayer()] ?: return

                val pending = data.pendingRequests
                if (pending.isEmpty) {
                    event.isCancelled = true
                    return
                }

                val id = WrapperPlayClientPong(event).id
                if (pending.firstInt() == id) {
                    pending.dequeueInt()
                    return
                }

                val first = pending.indexOf(id)
                if (first != -1) {
                    // Sync to this state
                    pending.dropFirst(first + 1)
                    if (data.waitingForSync) {
                        data.waitingForSync = false
                        return
                    }
                }

                if (!data.waitingForSync) {
                    event.isCancelled = true
                }
            }
        }
    }

}