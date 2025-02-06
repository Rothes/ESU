package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.data
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.locale
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.module.CommonModule
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.min

object HighLatencyAdjust: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    val adjusted = hashMapOf<Player, Int>()
    var task: ScheduledTask? = null

    init {
        for ((uuid, v) in data.originalViewDistance.entries) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null)
                adjusted[player] = v
        }
        data.originalViewDistance.clear()
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        adjusted.remove(e.player)
    }

    fun onEnable() {
        if (config.highLatencyAdjust.enabled) {
            task = Scheduler.async(0, 20 * 20) {
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.ping > config.highLatencyAdjust.latencyThreshold) {
                        if (!adjusted.containsKey(player)) {
                            adjusted[player] = player.clientViewDistance
                            player.user.message(locale, { highLatencyAdjust.adjustedWarning })
                            player.sendViewDistance = min(player.clientViewDistance, player.viewDistance) - 1
                        } else {
                            if (player.sendViewDistance > config.highLatencyAdjust.minViewDistance) {
                                player.sendViewDistance--
                            }
                        }
                    }
                }
            }
            PacketEvents.getAPI().eventManager.registerListener(this)
            Bukkit.getPluginManager().registerEvents(this, plugin)
        }
    }

    fun onDisable() {
        if (plugin.isEnabled || plugin.disabledHot) {
            for ((player, v) in adjusted.entries) {
                data.originalViewDistance[player.uniqueId] = v
            }
        }
        task?.cancel()
        task = null
        PacketEvents.getAPI().eventManager.unregisterListener(this)
        HandlerList.unregisterAll(this)
    }

    override fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packetType) {
            PacketType.Play.Client.CLIENT_SETTINGS -> {
                val wrapper = WrapperPlayClientSettings(event)
                val player = event.getPlayer<Player>()

                val old = adjusted[player] ?: return
                val viewDistance = wrapper.viewDistance
                if (old != viewDistance) {
                    // They have changed the setting
                    adjusted.remove(player)
                    player.sendViewDistance = -1
                }
            }
        }
    }
}