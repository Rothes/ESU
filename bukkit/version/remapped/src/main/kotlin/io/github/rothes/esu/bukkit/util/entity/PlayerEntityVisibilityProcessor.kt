package io.github.rothes.esu.bukkit.util.entity

import io.github.rothes.esu.bukkit.util.collect.FastIteLinkedQueue
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.delayedTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityValidTester
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.PlayerEntityVisibilityHandler
import io.github.rothes.esu.core.util.extension.math.square
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
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

    val trackedEntities = FastIteLinkedQueue<TrackedEntity>()
    val trackAllowedBuffer = IntOpenHashSet(4) // Buffer for UserTrackEntityEvent to skip #shouldHideDefault()

    abstract val updateIntervalTicks: Long

    open fun shouldHideDefault(entity: Entity): Boolean = true
    abstract fun shouldHide(entity: Entity, distSqr: Double): Boolean

    fun start() {
        task = player.syncTick {
            for (chunk in player.sentChunks) {
                val entities = chunk.entities
                for (entity in entities) {
                    if (entity is Player) continue
                    // We don't need to add this entity to queue.
                    // PlayerTrackEntityEvent triggers when it starts to track.
                    if (!entity.trackedBy.contains(player)) continue

                    // We don't process visibility here; Let #update() do it.
                    trackedEntities.add(TrackedEntity(entity))
                }
            }
            listener.register(plugin)
            schedule()
        }
    }

    protected open fun setupUpdate() {
        trackAllowedBuffer.clear() // Clear buffer in case of some entries are still in there
    }

    open fun update() {
        setupUpdate()
        val viewDist = player.sendViewDistance + 1 // Add 1 to debounce
        val maxSqrDist = (viewDist shl 4).square() shl 1 // Diagonal

        val playerHandle = HANDLE_GETTER.getHandle(player)
        val level = LEVEL_GETTER.level(playerHandle)
        val pos = playerHandle.position()

        val iterator = trackedEntities.iterator()
        for (te in iterator) {
            val bukkit = te.bukkitEntity
            val handle = HANDLE_GETTER.getHandle(bukkit)
            if (!VALID_TESTER.isValid(handle)) {
                // The entity has been removed from the world
                iterator.remove()
                continue
            }
            if (LEVEL_GETTER.level(handle) !== level) {
                // Entity is not on same world anymore
                VISIBILITY_HANDLER.forceShowEntity(player, bukkit, plugin)
                iterator.remove()
                continue
            }
            val other = handle.position()
            val xzDist = (pos.x - other.x).square() + (pos.z - other.z).square()
            if (xzDist > maxSqrDist) {
                // Entity is out of player visible distance
                VISIBILITY_HANDLER.showEntity(player, bukkit, plugin)
                iterator.remove()
                continue
            }

            if (shouldHide(bukkit, xzDist + (pos.y - other.y).square())) {
                if (!te.hidden) {
                    player.hideEntity(plugin, bukkit)
                    te.hidden = true
                }
            } else {
                if (te.hidden) {
                    trackAllowedBuffer.add(handle.id)
                    player.showEntity(plugin, bukkit)
                    te.hidden = false
                }
            }
        }
    }

    fun shutdown() {
        listener.unregister()
        player.syncTick {
            task?.cancel()
            task = null
            for (te in trackedEntities) {
                if (te.hidden && VALID_TESTER.isValid(te.bukkitEntity)) {
                    VISIBILITY_HANDLER.showEntity(player, te.bukkitEntity, plugin)
                }
            }
            trackedEntities.clear()
            trackAllowedBuffer.clear()
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

    data class TrackedEntity(
        @JvmField val bukkitEntity: Entity,
        @JvmField var hidden: Boolean = false,
    )

    inner class TrackListener: Listener {
        @EventHandler
        fun onTrackEntity(e: PlayerTrackEntityEvent) {
            if (e.player !== player) return
            if (e.entity is Player) return // Do not hide players, while this removes them from tab list
            if (trackAllowedBuffer.remove(e.entity.entityId)) return
            val hidden = shouldHideDefault(e.entity)
            if (hidden) {
                player.hideEntity(plugin, e.entity)
                e.isCancelled = true
            }
            trackedEntities.add(TrackedEntity(e.entity, hidden))
        }
    }

    companion object {
        private val VISIBILITY_HANDLER by Versioned(PlayerEntityVisibilityHandler::class.java)
        private val VALID_TESTER by Versioned(EntityValidTester::class.java)
        private val HANDLE_GETTER by Versioned(EntityHandleGetter::class.java)
        private val LEVEL_GETTER by Versioned(LevelHandler::class.java)
    }

}