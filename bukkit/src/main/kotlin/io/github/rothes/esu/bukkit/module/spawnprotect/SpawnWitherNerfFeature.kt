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

package io.github.rothes.esu.bukkit.module.spawnprotect

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import io.github.rothes.esu.core.configuration.LoadedConfiguration
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Entity
import org.bukkit.entity.Wither
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.world.ChunkLoadEvent
import kotlin.math.abs
import kotlin.math.max

object SpawnWitherNerfFeature: CommonFeature<SpawnWitherNerfFeature.FeatureConfig, Unit>() {

    private val witherNerfKey = NamespacedKey.fromString("wither-mod", plugin)!!

    override fun onEnable() {
        Listeners.register()
        update()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
        update()
    }

    override fun onReload() {
        super.onReload()
        if (enabled) update()
    }

    fun update() {
        for (world in Bukkit.getWorlds()) {
            for (chunk in world.loadedChunks) {
                chunk.getBlock(0, 0, 0).location.syncTick {
                    for (entity in chunk.entities) {
                        handleEntity(entity)
                    }
                }
            }
        }
    }

    fun handleEntity(e: Entity) {
        if (e !is Wither) return

        resetEntity(e)
        val nerf = config.rules.firstOrNull { it.radius >= max(abs(e.location.x), abs(e.location.z)) } ?: return

        e.getAttribute(AttributeAdapter.FLYING_SPEED)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey, -1 + nerf.speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
        e.getAttribute(AttributeAdapter.MOVEMENT_SPEED)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey,
                -1 + nerf.speedModifier,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
        e.getAttribute(AttributeAdapter.FOLLOW_RANGE)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey,
                -1 + nerf.followRangeModifier,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
    }

    fun resetEntity(e: Entity) {
        if (e !is Wither) return

        e.getAttribute(AttributeAdapter.FLYING_SPEED)?.removeModifier(witherNerfKey)
        e.getAttribute(AttributeAdapter.MOVEMENT_SPEED)?.removeModifier(witherNerfKey)
        e.getAttribute(AttributeAdapter.FOLLOW_RANGE)?.removeModifier(witherNerfKey)
    }

    override fun configNode(base: LoadedConfiguration): LoadedConfiguration {
        val node = base.node
        node.node("spawn-wither-nerf")?.let { base ->
            if (base.isList) {
                // ESU v1.0.1 schema
                val raw = base.raw()
                val rules = base.node("rules")
                if (rules.isNull) {
                    rules.raw(raw)
                }
            }
        }
        return super.configNode(base)
    }

    private object Listeners: Listener {

        @EventHandler
        fun onEntitySpawn(e: EntitySpawnEvent) {
            if (e.entity !is Wither)
                return
            val nerf = config.rules.firstOrNull { it.radius >= max(abs(e.location.x), abs(e.location.z)) } ?: return

            val amount = e.entity.chunk.entities.count { it is Wither }
            if (amount >= nerf.maxAmount) {
                e.isCancelled = true
                return
            }
            handleEntity(e.entity)
        }

        @EventHandler
        fun onChunkLoad(e: ChunkLoadEvent) {
            val nerf = config.rules.firstOrNull { it.radius shr 4 >= max(abs(e.chunk.x), abs(e.chunk.z)) } ?: return

            val withers = e.chunk.entities.filterIsInstance<Wither>()
            val max = nerf.maxAmount
            val amount = withers.size
            if (amount > max) {
                for (entity in withers.takeLast(amount - max)) entity.remove()
                for (entity in withers.dropLast(amount - max)) handleEntity(entity)
                return
            }
            for (entity in withers) handleEntity(entity)
        }

    }

    data class FeatureConfig(
        val rules: List<SpawnWitherNerf> = listOf(SpawnWitherNerf(256, 0), SpawnWitherNerf())
    ) : BaseFeatureConfiguration(true) {
        data class SpawnWitherNerf(
            val radius: Long = 1024,
            val maxAmount: Int = 1,
            val speedModifier: Double = 0.5,
            val followRangeModifier: Double = 0.25,
        )
    }

}