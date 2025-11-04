package io.github.rothes.esu.bukkit.module.networkthrottle.v1_17_1

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.util.UnsafeUtils.usIntAccessor
import net.minecraft.server.level.ServerEntity
import net.minecraft.world.entity.EntityType

class EntityUpdateIntervalImpl: EntityUpdateInterval<EntityUpdateIntervalImpl.FeatureConfig, Unit>() {

    companion object {
        val entityTypeAccessor = EntityType::class.java.getDeclaredField("updateInterval").usIntAccessor
        val serverEntityAccessor = ServerEntity::class.java.getDeclaredField("updateInterval").usIntAccessor
    }

    override fun onReload() {
        super.onReload()
        if (enabled) {
            applyUpdateInterval()
        }
    }

    override fun onEnable() {
        applyUpdateInterval()
    }

    private fun applyUpdateInterval() {
        val config = config
        for ((type, interval) in config.entityTypeUpdateInterval) {
            entityTypeAccessor[type] = interval
        }
    }

    data class FeatureConfig(
        @Comment("""
            Control the position update interval ticks of entity types.
            Higher means less entity movement packets (more de-sync), but less network as deal.
        """)
        val entityTypeUpdateInterval: Map<EntityType<*>, Int> = mapOf(
            EntityType.PIG to entityTypeAccessor[EntityType.PIG],
            EntityType.ZOMBIE to entityTypeAccessor[EntityType.ZOMBIE],
        )
    ): BaseFeatureConfiguration()
}