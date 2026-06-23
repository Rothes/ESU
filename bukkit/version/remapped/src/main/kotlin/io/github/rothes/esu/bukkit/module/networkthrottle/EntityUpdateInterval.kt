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

package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelEntitiesHandler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.user.User
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftWorld
import org.incendo.cloud.annotations.Command

abstract class EntityUpdateInterval: CommonFeature<EntityUpdateInterval.FeatureConfig, Unit>() {

    companion object {
        val INSTANCE = versioned<EntityUpdateInterval>()
    }

    override val name: String = "EntityUpdateInterval"

    private var previousConfig: FeatureConfig? = null

    override fun onReload() {
        super.onReload()
        if (enabled) applyUpdateInterval()
    }

    override fun onEnable() {
        applyUpdateInterval()

        registerCommands(object {
            @Command("esu networkThrottle entityUpdateInterval entityType <entityType>")
            @ShortPerm
            fun getUpdateInterval(sender: User, entityType: EntityType<*>) {
                val interval = this@EntityUpdateInterval[entityType]
                sender.miniMessage("<pc>Current update interval of entity type <pdc>${entityType.toShortString()}</pdc> is <pdc>$interval")
            }

            @Command("esu networkThrottle entityUpdateInterval updateTrackedEntities")
            @ShortPerm
            fun updateTrackedEntities(sender: User) {
                if (!ServerInfo.isPaper) {
                    sender.message("§cNot supported on Spigot yet")
                    return
                }
                val handler = versioned<TrackedEntityIntervalUpdater>()
                val updated = handler.updateTrackedEntities()
                sender.message("Updated $updated entities")
            }
        })
    }

    abstract operator fun get(entityType: EntityType<*>): Int
    abstract operator fun set(entityType: EntityType<*>, interval: Int)

    protected fun applyUpdateInterval() {
        val config = config
        if (config != previousConfig) {
            for ((type, interval) in config.entityTypeUpdateInterval) {
                this[type] = interval
            }
            // TODO: We could add Spigot support for this but it requires special source imported server
            if (ServerInfo.isPaper) versioned<TrackedEntityIntervalUpdater>().updateTrackedEntities()
        }
        previousConfig = config
    }

    data class FeatureConfig(
        @Comment("""
            Control the position update interval ticks of entity types.
            Higher means less entity movement packets (more de-sync), but less network as deal.
        """)
        val entityTypeUpdateInterval: Map<EntityType<*>, Int> = mapOf(
            EntityTypes.PIG to INSTANCE[EntityTypes.PIG],
            EntityTypes.ZOMBIE to INSTANCE[EntityTypes.ZOMBIE],
        )
    ): BaseFeatureConfiguration()

    abstract class TrackedEntityIntervalUpdater {

        fun updateTrackedEntities(): Int {
            val entitiesHandler = versioned<LevelEntitiesHandler>()

            var updated = 0
            for (level in Bukkit.getWorlds().map { it as CraftWorld }.map { it.handle }) {
                for (entity in entitiesHandler.getEntitiesAll(level)) {
                    if (handleEntity(entity)) updated++
                }
            }
            return updated
        }

        abstract fun handleEntity(entity: Entity): Boolean

    }

}