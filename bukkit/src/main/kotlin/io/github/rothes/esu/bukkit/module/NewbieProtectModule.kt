package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.entity.CraftWither
import org.bukkit.entity.Entity
import org.bukkit.entity.Wither
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.world.ChunkLoadEvent
import kotlin.math.abs
import kotlin.math.max

object NewbieProtectModule: BukkitModule<NewbieProtectModule.ModuleConfig, EmptyConfiguration>(
    ModuleConfig::class.java, EmptyConfiguration::class.java
) {

    override fun enable() {
        update()
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
    }

    override fun disable() {
        super.disable()
        if (plugin.isEnabled) {
            // This can only run when enabled
            for (world in Bukkit.getWorlds()) {
                for (chunk in world.loadedChunks) {
                    Scheduler.schedule(chunk.getBlock(0, 0, 0).location, plugin) {
                        for (entity in chunk.entities) {
                            resetEntity(entity)
                        }
                    }
                }
            }
        }
        HandlerList.unregisterAll(Listeners)
    }

    override fun reloadConfig() {
        super.reloadConfig()
        if (enabled) {
            update()
        }
    }

    fun update() {
        for (world in Bukkit.getWorlds()) {
            for (chunk in world.loadedChunks) {
                Scheduler.schedule(chunk.getBlock(0, 0, 0).location, plugin) {
                    for (entity in chunk.entities) {
                        handleEntity(entity)
                    }
                }
            }
        }
    }

    private val witherNerfKey = NamespacedKey.fromString("wither-mod", plugin)!!
    fun handleEntity(e: Entity) {
        if (e !is CraftWither) return

        resetEntity(e)
        if (max(abs(e.location.x), abs(e.location.z)) > config.spawnWitherNerf.radius) return

        e.getAttribute(Attribute.GENERIC_FLYING_SPEED)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey,
                -1 + config.spawnWitherNerf.speedModifier,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
        e.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey,
                -1 + config.spawnWitherNerf.speedModifier,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
        e.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey,
                -1 + config.spawnWitherNerf.followRangeModifier,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
    }

    fun resetEntity(e: Entity) {
        if (e !is CraftWither) return

        e.getAttribute(Attribute.GENERIC_FLYING_SPEED)?.removeModifier(witherNerfKey)
        e.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.removeModifier(witherNerfKey)
        e.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)?.removeModifier(witherNerfKey)
    }

    object Listeners: Listener {

        @EventHandler
        fun onEntitySpawn(e: EntitySpawnEvent) {
            if (e.entity !is Wither)
                return
            if (max(abs(e.location.x), abs(e.location.z)) > config.spawnWitherNerf.radius) return

            val amount = e.entity.chunk.entities.filter { it is Wither }.size
            if (amount >= config.spawnWitherNerf.maxAmount) {
                e.isCancelled = true
                return
            }
            handleEntity(e.entity)
        }

        @EventHandler
        fun onChunkLoad(e: ChunkLoadEvent) {
            for (entity in e.chunk.entities) {
                handleEntity(entity)
            }
        }
    }

    data class ModuleConfig(
        val spawnWitherNerf: SpawnWitherNerf = SpawnWitherNerf(),
    ): BaseModuleConfiguration() {

        data class SpawnWitherNerf(
            val radius: Long = 512,
            val maxAmount: Int = 1,
            val speedModifier: Double = 0.5,
            val followRangeModifier: Double = 0.25,
        )
    }

}