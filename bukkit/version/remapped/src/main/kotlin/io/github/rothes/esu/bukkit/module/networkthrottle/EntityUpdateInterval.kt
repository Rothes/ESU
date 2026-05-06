package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelEntitiesHandler
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.user.User
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftWorld
import org.incendo.cloud.annotations.Command

abstract class EntityUpdateInterval: CommonFeature<EntityUpdateInterval.FeatureConfig, Unit>() {

    companion object {
        val INSTANCE by Versioned(EntityUpdateInterval::class.java)
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
                val handler by Versioned(TrackedEntityIntervalUpdater::class.java)
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
            TrackedEntityIntervalUpdater::class.java.versioned().updateTrackedEntities()
        }
        previousConfig = config
    }

    data class FeatureConfig(
        @Comment("""
            Control the position update interval ticks of entity types.
            Higher means less entity movement packets (more de-sync), but less network as deal.
        """)
        val entityTypeUpdateInterval: Map<EntityType<*>, Int> = mapOf(
            EntityType.PIG to INSTANCE[EntityType.PIG],
            EntityType.ZOMBIE to INSTANCE[EntityType.ZOMBIE],
        )
    ): BaseFeatureConfiguration()

    abstract class TrackedEntityIntervalUpdater {

        fun updateTrackedEntities(): Int {
            val entitiesHandler by Versioned(LevelEntitiesHandler::class.java)

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