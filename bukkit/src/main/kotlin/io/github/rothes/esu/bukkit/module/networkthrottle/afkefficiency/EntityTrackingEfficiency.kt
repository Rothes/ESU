/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency

import io.github.rothes.esu.bukkit.core
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
import io.github.rothes.esu.lib.configurate.objectmapping.meta.PostProcess
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

    private class VisibilityProcessor(player: Player): PlayerEntityVisibilityProcessor.SyncTick(player, pl) {

        override val updateIntervalTicks: Long
            get() = config.updateIntervalTicks

        override fun shouldHide(entity: Entity, distSqr: Double): HideState {
            return if (distSqr >= config.visibleEntityDistanceSquared) HideState.HIDE else HideState.SHOW
        }

    }

    @Comment("""
        Hide all far away entities from player.
    """)
    data class FeatureConfig(
        @Comment("Player could see nearby entities in this distance")
        val visibleEntityDistance: Double = 7.0,
        @Comment("Server game tick interval to update player nearby entities")
        var updateIntervalTicks: Long = 5,
    ): BaseFeatureConfiguration(true) {

        val visibleEntityDistanceSquared by lazy { square(visibleEntityDistance) }

        @PostProcess
        private fun pp() {
            if (updateIntervalTicks < 1) {
                core.warn("[$name] UpdateIntervalTicks must be > 0 !")
                updateIntervalTicks = 1
            }
        }
    }

}