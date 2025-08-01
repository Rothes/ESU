package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.data
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.locale
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.min

object HighLatencyAdjust: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    private const val NO_TIME = -1L

    val adjusted = hashMapOf<Player, Int>()
    val startTime = Object2LongOpenHashMap<Player>().also { it.defaultReturnValue(NO_TIME) }
    var task: ScheduledTask? = null

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        adjusted.remove(e.player)
        startTime.removeLong(e.player)
    }

    fun onEnable() {
        if (!ServerCompatibility.paper) {
            plugin.err("[HighLatencyAdjust] This feature requires Paper server")
            return
        }
        for ((uuid, v) in data.originalViewDistance.entries) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null)
                adjusted[player] = v
        }
        data.originalViewDistance.clear()

        if (config.highLatencyAdjust.enabled) {
            task = Scheduler.async(0, 15 * 20) {
                val now = System.currentTimeMillis()
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.ping >= config.highLatencyAdjust.latencyThreshold) {
                        val last = startTime.getLong(player)
                        if (last == NO_TIME) {
                            startTime[player] = now
                            continue
                        } else if ((now - last) < config.highLatencyAdjust.duration.toMillis()) {
                            continue
                        }
                        startTime.removeLong(player)

                        if (!adjusted.containsKey(player)) {
                            adjusted[player] = player.clientViewDistance
                            player.sendViewDistance = min(player.clientViewDistance, player.viewDistance) - 1
                        } else {
                            if (player.sendViewDistance <= config.highLatencyAdjust.minViewDistance) {
                                continue
                            }
                            player.sendViewDistance--
                        }
                        player.user.message(locale, { highLatencyAdjust.adjustedWarning })
                    } else {
                        startTime.removeLong(player)
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
        adjusted.clear()
        startTime.clear()
    }

    override fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packetType) {
            PacketType.Play.Client.CLIENT_SETTINGS -> {
                val wrapper = WrapperPlayClientSettings(event)
                val player = event.getPlayer<Player>()

                val old = adjusted[player] ?: return
                val viewDistance = wrapper.viewDistance
                if (!config.highLatencyAdjust.newViewDistanceToReset || old != viewDistance) {
                    // They have changed the setting
                    adjusted.remove(player)
                    startTime.removeLong(player)
                    player.sendViewDistance = -1
                }
            }
        }
    }
}