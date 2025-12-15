package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.data
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.coroutine.AsyncScope
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Duration
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object HighLatencyAdjust: CommonFeature<HighLatencyAdjust.FeatureConfig, HighLatencyAdjust.FeatureLang>(), Listener {

    private const val NO_TIME = -1L

    val adjusted = hashMapOf<Player, Int>()
    val startTime = Object2LongOpenHashMap<Player>().also { it.defaultReturnValue(NO_TIME) }
    var task: Job? = null

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        adjusted.remove(e.player)
        startTime.removeLong(e.player)
    }

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents() ?: let {
            if (!ServerCompatibility.isPaper) {
                plugin.err("[HighLatencyAdjust] This feature requires Paper server")
                return Feature.AvailableCheck.fail { "This feature requires Paper server".message }
            }
            null
        }
    }

    override fun onEnable() {
        for ((uuid, v) in data.originalViewDistance.entries) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null)
                adjusted[player] = v
        }
        data.originalViewDistance.clear()

        task = AsyncScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.ping >= config.latencyThreshold) {
                        val last = startTime.getLong(player)
                        if (last == NO_TIME) {
                            startTime[player] = now
                            continue
                        } else if ((now - last) < config.duration.toMillis()) {
                            continue
                        }
                        startTime.removeLong(player)

                        if (!adjusted.containsKey(player)) {
                            adjusted[player] = player.clientViewDistance
                            player.sendViewDistance = min(player.clientViewDistance, player.viewDistance) - 1
                        } else {
                            if (player.sendViewDistance <= config.minViewDistance) {
                                continue
                            }
                            player.sendViewDistance--
                        }
                        player.user.message(lang, { adjustedWarning })
                    } else {
                        startTime.removeLong(player)
                    }
                }
                delay(15.seconds)
            }
        }
        PacketEvents.getAPI().eventManager.registerListener(PacketListeners)
        register()
    }

    override fun onDisable() {
        if (plugin.isEnabled || plugin.disabledHot) {
            for ((player, v) in adjusted.entries) {
                data.originalViewDistance[player.uniqueId] = v
            }
        }
        task?.cancel()
        task = null
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListeners)
        unregister()
        adjusted.clear()
        startTime.clear()
    }

    @Comment("""
            Adjust the settings the players with high latency to lower value.
            So they won't affect average quality of all players.
            """)
    data class FeatureConfig(
        @Comment("Trigger a adjust when player's ping is greater than or equal this.")
        val latencyThreshold: Int = 150,
        @Comment("The high ping must keep for the duration to trigger a adjust finally.")
        val duration: Duration = kotlin.time.Duration.parse("1m").toJavaDuration(),
        @Comment("Plugin detects CLIENT_SETTINGS packets to reset the view distance for players.\n" +
                "If true, player must change the client view distance for a reset;\n" +
                "If false, any new settings could reset the view distance for the player.")
        val newViewDistanceToReset: Boolean = false,
        val minViewDistance: Int = 5,
    ): BaseFeatureConfiguration()
    
    data class FeatureLang(
        val adjustedWarning: MessageData = ("<ec><b>Warning: </b><pc>Your network latency seems to be high. \n" +
                "To enhance your experience, we have adjusted your view distance. " +
                "You can always adjust it yourself in the game options.").message,
    ): ConfigurationPart
    
    private object PacketListeners: PacketListenerAbstract(PacketListenerPriority.HIGHEST) {

        override fun onPacketReceive(event: PacketReceiveEvent) {
            when (event.packetType) {
                PacketType.Play.Client.CLIENT_SETTINGS -> {
                    val wrapper = WrapperPlayClientSettings(event)
                    val player = event.getPlayer<Player>()

                    val old = adjusted[player] ?: return
                    val viewDistance = wrapper.viewDistance
                    if (!config.newViewDistanceToReset || old != viewDistance) {
                        // They have changed the setting
                        adjusted.remove(player)
                        startTime.removeLong(player)
                        player.sendViewDistance = -1
                    }
                }
            }
        }
    }
    
}