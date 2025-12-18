package io.github.rothes.esu.bukkit.module.networkthrottle

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.module.CoreModule
import io.github.rothes.esu.bukkit.module.core.PlayerTimeProvider
import io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency.AfkEfficiencyFeature
import io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency.EntityTrackingEfficiency
import io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency.LimitedPacketEfficiency
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object AfkEfficiency: CommonFeature<AfkEfficiency.FeatureConfig, AfkEfficiency.FeatureLang>() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val playerMap = ConcurrentHashMap<Player, PlayerHolder>()

    private val efficiencyFeatures
        get() = children.values.filterIsInstance<AfkEfficiencyFeature<*, *>>().filter { it.enabled }

    val efficiencyPlayers
        get() = playerMap.values

    init {
        registerFeature(EntityTrackingEfficiency)
        registerFeature(LimitedPacketEfficiency)
    }

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (!CoreModule.enabled) return errFail { "This feature required CoreModule enabled".message }
            null
        }
    }

    override fun onReload() {
        super.onReload()
        if (enabled) {
            for (holder in playerMap.values) {
                holder.reschedule()
            }
        }
    }

    override fun onEnable() {
        for (player in Bukkit.getOnlinePlayers()) {
            playerMap[player] = PlayerHolder(player)
        }
        PlayerListeners.register()
        SpectateListeners.listener.register()
        CoreModule.providers.posMoveTime.registerListener(PlayerMoveListener)
    }

    override fun onDisable() {
        super.onDisable()
        PlayerListeners.unregister()
        SpectateListeners.listener.unregister()
        CoreModule.providers.posMoveTime.unregisterListener(PlayerMoveListener)
        for (holder in playerMap.values) {
            holder.shutdown()
        }
        playerMap.clear()
    }

    class PlayerHolder(
        val player: Player,
    ) {
        val user = player.user
        var inAfk: Boolean = false
        private var afkTask: Job? = null

        init {
            reschedule()
        }

        @Synchronized
        fun reschedule(lastAction: Long = CoreModule.providers.posMoveTime[player]) {
            cancel()
            afkTask = createAfkTask(lastAction)
        }

        @Synchronized
        fun cancel() {
            afkTask?.cancel()
            afkTask = null
        }

        @Synchronized
        fun shutdown() {
            disableEfficiency()
            cancel()
        }

        @Synchronized
        fun enableEfficiency() {
            if (inAfk) return
            cancel()
            inAfk = true
            for (feature in efficiencyFeatures) {
                try {
                    feature.onEnableEfficiency(this)
                } catch (e: Throwable) {
                    core.err("[AfkEfficiency] An error occurred while enabling ${feature.name}", e)
                }
            }
            user.message(lang, { afkEfficiencyEnabled })
        }

        @Synchronized
        fun disableEfficiency(delta: Long = CoreModule.providers.posMoveTime[player]) {
            if (inAfk) {
                inAfk = false
                for (feature in efficiencyFeatures) {
                    try {
                        feature.onDisableEfficiency(this)
                    } catch (e: Throwable) {
                        core.err("[AfkEfficiency] An error occurred while disabling ${feature.name}", e)
                    }
                }
                user.message(lang, { afkEfficiencyDisabled })
            }
            reschedule(delta)
        }

        private fun createAfkTask(lastAction: Long): Job {
            return coroutineScope.launch {
                val afkTime = System.currentTimeMillis() - lastAction
                delay(config.afkDuration.toMillis() - afkTime)
                if (!isActive) return@launch
                enableEfficiency()
            }
        }

    }

    private object PlayerListeners: Listener {

        @EventHandler
        fun onJoin(event: PlayerJoinEvent) {
            playerMap[event.player] = PlayerHolder(event.player)
        }

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            playerMap.remove(event.player)?.shutdown()
        }
    }

    // We don't want a player goes into efficiency mode while spectating others, this listener is to solve the issue.
    private object SpectateListeners {

        val listener = if (ServerCompatibility.isPaper) Paper else CB

        private object CB: Listener {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun onSpectate(event: PlayerTeleportEvent) {
                if (event.cause != PlayerTeleportEvent.TeleportCause.SPECTATE) return
                playerMap[event.player]?.cancel()
            }
        }

        private object Paper: Listener {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun onSpectate(event: PlayerStartSpectatingEntityEvent) {
                playerMap[event.player]?.cancel()
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun onSpectate(event: PlayerStopSpectatingEntityEvent) {
                playerMap[event.player]?.reschedule()
            }
        }

    }

    private object PlayerMoveListener: PlayerTimeProvider.ChangeListener {

        override fun onTimeChanged(player: Player, oldTime: Long, newTime: Long) {
            playerMap[player]?.disableEfficiency(newTime)
        }

    }

    @Comment("""
        Limit packets send to afk players to reduce upload bandwidth usage.
        Players will see delayed/desync world during afk efficiency mode.
    """)
    data class FeatureConfig(
        @Comment("""
            The duration player must afk for to trigger afk efficiency mode.
        """)
        val afkDuration: Duration = Duration.ofMinutes(2)
    ): BaseFeatureConfiguration()

    data class FeatureLang(
        val afkEfficiencyEnabled: MessageData = "<tc>Efficiency mode activated, world changes are delayed.<title:0:365d:0><subtitle><tc>Efficiency mode activating".message,
        val afkEfficiencyDisabled: MessageData = "<title:0:1s:0.5s><subtitle><pdc>Efficiency mode disabled<sound:minecraft:block.note_block.bell:master:0.5>".message,
    )

}