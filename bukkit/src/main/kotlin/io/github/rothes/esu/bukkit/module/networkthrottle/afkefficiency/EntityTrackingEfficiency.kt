package io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency

import io.github.rothes.esu.bukkit.module.networkthrottle.AfkEfficiency
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.entity.PlayerEntityVisibilityProcessor
import io.github.rothes.esu.bukkit.util.extension.createChild
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.util.extension.math.square
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

object EntityTrackingEfficiency: AfkEfficiencyFeature<EntityTrackingEfficiency.FeatureConfig, Unit>() {

    private val playerData = ConcurrentHashMap<Player, VisibilityProcessor>()
    private val pl = plugin.createChild(name = "${plugin.name}-$name")

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (ServerCompatibility.serverVersion < 19 || !ServerCompatibility.isPaper)
                return errFail { "Paper 1.19+ is required to enable this.".message }
            null
        }
    }

    override fun onDisable() {
        super.onDisable()
        for (data in playerData.values) {
            data.shutdown()
        }
        playerData.clear()
    }

    override fun onEnableEfficiency(playerHolder: AfkEfficiency.PlayerHolder) {
        val player = playerHolder.player
        playerData[player] = VisibilityProcessor(player).also { it.start() }
    }

    override fun onDisableEfficiency(playerHolder: AfkEfficiency.PlayerHolder) {
        playerData.remove(playerHolder.player)?.shutdown()
    }

    private class VisibilityProcessor(player: Player): PlayerEntityVisibilityProcessor(player, pl) {

        override val updateIntervalTicks: Long
            get() = config.updateIntervalTicks

        override fun shouldHide(entity: Entity, distSqr: Double): Boolean {
            return distSqr >= config.visibleEntityDistanceSquared
        }

    }

    @Comment("""
        Hide all far away entities from player.
    """)
    data class FeatureConfig(
        @Comment("Player could see nearby entities in this distance")
        val visibleEntityDistance: Double = 7.0,
        @Comment("Server game tick interval to update player nearby entities")
        val updateIntervalTicks: Long = 5,
    ): BaseFeatureConfiguration(true) {

        val visibleEntityDistanceSquared by lazy { visibleEntityDistance.square() }
    }

}