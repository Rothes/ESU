package io.github.rothes.esu.bukkit.module.anticheat.movement

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
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object MultiPlayerInput : CommonFeature<FeatureToggle.DefaultTrue, Unit>() {

    private val playerMap = ConcurrentHashMap<Player, PlayerData>()

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents()
    }

    override fun onEnable() {
        BukkitListener.register()
        for (player in Bukkit.getOnlinePlayers()) {
            playerMap[player] = PlayerData()
        }
        PrePacketEventManager.eventManager.registerListener(Listener)
    }

    override fun onDisable() {
        super.onDisable()
        PrePacketEventManager.eventManager.unregisterListener(Listener)
        BukkitListener.unregister()
        playerMap.clear()
    }


    private data class PlayerData(
        var sentInputThisTick: Boolean = false,
    )

    private object BukkitListener : org.bukkit.event.Listener {

        @EventHandler
        fun onJoin(event: PlayerJoinEvent) {
            playerMap[event.player] = PlayerData()
        }

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            playerMap.remove(event.player)
        }
    }

    private object Listener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketReceive(event: PacketReceiveEvent) {
            when (event.packetType) {
                PacketType.Play.Client.CLIENT_TICK_END -> {
                    val data = playerMap[event.getPlayer()] ?: return
                    data.sentInputThisTick = false
                }
                PacketType.Play.Client.PLAYER_INPUT -> {
                    val data = playerMap[event.getPlayer()] ?: return
//                    val flags = ByteBufHelper.readByte(event.byteBuf)
                    if (data.sentInputThisTick) {
                        event.isCancelled = true
                    } else {
                        data.sentInputThisTick = true
                    }
                }
            }
        }
    }

}