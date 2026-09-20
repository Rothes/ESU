package io.github.rothes.esu.bukkit.module.anticheat.movement

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction
import io.github.rothes.esu.bukkit.module.anticheat.PrePacketEventManager
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object ElytraStartGlideInterval : CommonFeature<ElytraStartGlideInterval.FeatureConfig, Unit>() {

    private val playerMap = ConcurrentHashMap<Player, Long>()

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents()
    }

    override fun onEnable() {
        Listeners.register()
        PrePacketEventManager.eventManager.registerListener(PacketListener)
    }

    override fun onDisable() {
        super.onDisable()
        PrePacketEventManager.eventManager.unregisterListener(PacketListener)
        Listeners.unregister()
        playerMap.clear()
    }

    private object Listeners : Listener {

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            playerMap.remove(event.player)
        }

    }

    private object PacketListener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketReceive(event: PacketReceiveEvent) {
            if (event.packetType == PacketType.Play.Client.ENTITY_ACTION) {
                val wrapper = WrapperPlayClientEntityAction(event)
                if (wrapper.action == WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA) {
                    val player = event.getPlayer<Player>()
                    val now = System.currentTimeMillis()
                    val previous = playerMap.put(player, System.currentTimeMillis()) ?: return
                    if (now - previous < config.minInterval.toMillis()) {
                        event.isCancelled = true
                        player.isSneaking = !player.isSneaking // Resync client
                    }
                }
            }
        }
    }

    data class FeatureConfig(
        val minInterval: Duration = Duration.ofMillis(150),
    ) : BaseFeatureConfiguration(false)

}