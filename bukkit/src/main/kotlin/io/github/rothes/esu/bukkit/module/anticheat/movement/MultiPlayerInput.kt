package io.github.rothes.esu.bukkit.module.anticheat.movement

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import io.github.rothes.esu.bukkit.module.anticheat.PrePacketEventManager
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object MultiPlayerInput : CommonFeature<FeatureToggle.DefaultTrue, Unit>() {

    private val playerMap = ConcurrentHashMap<Player, Boolean>()

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents()
    }

    override fun onEnable() {
        BukkitListener.register()
        PrePacketEventManager.eventManager.registerListener(PreListener)
        PacketEvents.getAPI().eventManager.registerListener(PostListener)
    }

    override fun onDisable() {
        super.onDisable()
        PrePacketEventManager.eventManager.unregisterListener(PreListener)
        PacketEvents.getAPI().eventManager.unregisterListener(PostListener)
        BukkitListener.unregister()
        playerMap.clear()
    }

    private object BukkitListener : org.bukkit.event.Listener {

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            playerMap.remove(event.player)
        }
    }

    private object PreListener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketReceive(event: PacketReceiveEvent) {
            if (event.packetType == PacketType.Play.Client.PLAYER_INPUT) {
                if (playerMap.put(event.getPlayer(), true) != null) {
                    event.isCancelled = true
                }
            }
        }
    }

    private object PostListener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketReceive(event: PacketReceiveEvent) {
            if (event.packetType == PacketType.Play.Client.CLIENT_TICK_END) {
                playerMap.remove(event.getPlayer())
            }
        }
    }

}