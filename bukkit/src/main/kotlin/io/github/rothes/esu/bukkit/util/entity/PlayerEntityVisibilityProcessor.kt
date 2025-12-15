package io.github.rothes.esu.bukkit.util.entity

import io.github.rothes.esu.bukkit.event.UserTrackEntityEvent
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.PlayerEntityVisibilityHandler
import io.github.rothes.esu.bukkit.util.collect.FastIteLinkedQueue
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.delayedTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityValidTester
import io.github.rothes.esu.core.util.extension.math.square
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.logging.Level

abstract class PlayerEntityVisibilityProcessor(
    val player: Player,
    val plugin: Plugin,
) {

    @Volatile private var task: ScheduledTask? = null
    private val listener = TrackListener()

    val trackingEntities = FastIteLinkedQueue<Entity>()
    val hiddenEntities = FastIteLinkedQueue<Entity>()

    abstract val updateIntervalTicks: Long

    open fun shouldHide(entity: Entity): Boolean = shouldHide(entity, entity.location.distanceSquared(player.location))
    abstract fun shouldHide(entity: Entity, distSqr: Double): Boolean

    fun start() {
        task = player.syncTick {
            val loc = player.location
            for (chunk in player.sentChunks) {
                val entities = chunk.entities
                for (entity in entities) {
                    if (entity is Player) continue
                    if (shouldHide(entity, entity.location.distanceSquared(loc))) {
                        hiddenEntities.add(entity)
                        player.hideEntity(plugin, entity)
                    } else {
                        trackingEntities.add(entity)
                    }
                }
            }
            listener.register(plugin)
            schedule()
        }
    }

    open fun update() {
        val loc = player.location
        val viewDist = player.sendViewDistance + 1 // Add 1 to debounce
        val maxSqrDist = (viewDist shl 4).square() shl 1 // Diagonal
        val hidden = hiddenEntities.iterator()
        val tracking = trackingEntities.iterator()
        updateQueue(hidden, true, loc, maxSqrDist)
        updateQueue(tracking, false, loc, maxSqrDist)
    }

    private fun updateQueue(iterator: MutableIterator<Entity>, isHiddenQueue: Boolean, playerLoc: Location, maxSqrDist: Int) {
        for (entity in iterator) {
            if (!VALID_TESTER.isValid(entity)) {
                // The entity has been removed from the world
                iterator.remove()
                continue
            }
            val other = entity.location
            if (other.world !== playerLoc.world) {
                // Entity is not on same world anymore
                VISIBILITY_HANDLER.forceShowEntity(player, entity, plugin)
                iterator.remove()
                continue
            }
            val xzDist = (playerLoc.x - other.x).square() + (playerLoc.z - other.z).square()
            if (xzDist > maxSqrDist) {
                // Entity is out of player visible distance
                VISIBILITY_HANDLER.showEntity(player, entity, plugin)
                iterator.remove()
                continue
            }

            if (shouldHide(entity, xzDist + (playerLoc.y - other.y).square())) {
                if (!isHiddenQueue) {
                    player.hideEntity(plugin, entity)
                    trackingEntities.add(entity)
                    iterator.remove()
                }
            } else {
                if (isHiddenQueue) {
                    player.showEntity(plugin, entity)
                    hiddenEntities.add(entity)
                    iterator.remove()
                }
            }
        }
    }

    fun shutdown() {
        listener.unregister()
        player.syncTick {
            task?.cancel()
            task = null
            for (entity in hiddenEntities) {
                if (VALID_TESTER.isValid(entity)) {
                    VISIBILITY_HANDLER.showEntity(player, entity, plugin)
                }
            }
            hiddenEntities.clear()
            trackingEntities.clear()
        }
    }

    private fun schedule() {
        task = player.delayedTick(updateIntervalTicks) {
            try {
                update()
            } catch (e: Throwable) {
                plugin.logger.log(Level.SEVERE, "Failed to update ${player.name}", e)
            }
            schedule()
        } ?: error("Failed to schedule update task for player ${player.name}")
    }

    inner class TrackListener: Listener {
        @EventHandler
        fun onTrackEntity(e: UserTrackEntityEvent) {
            if (e.player !== player) return
            if (shouldHide(e.entity)) {
                hiddenEntities.add(e.entity)
                player.hideEntity(plugin, e.entity)
                e.isCancelled = true
            } else {
                trackingEntities.add(e.entity)
            }
        }
    }

    companion object {
        private val VISIBILITY_HANDLER by Versioned(PlayerEntityVisibilityHandler::class.java)
        private val VALID_TESTER by Versioned(EntityValidTester::class.java)
    }

}