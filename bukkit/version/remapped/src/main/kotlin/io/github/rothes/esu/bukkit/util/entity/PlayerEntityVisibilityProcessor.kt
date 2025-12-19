package io.github.rothes.esu.bukkit.util.entity

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.delayedTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.nextTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityValidTester
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.PlayerEntityVisibilityHandler
import io.github.rothes.esu.core.util.extension.math.square
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.logging.Level

abstract class PlayerEntityVisibilityProcessor(
    val player: Player,
    val plugin: Plugin,
) {

    protected var task: ScheduledTask? = null
    private val listener = TrackListener()

    protected val trackedEntities = Int2ReferenceOpenHashMap<TrackedEntity>()

    open fun shouldHideDefault(entity: Entity): Boolean = true
    open fun shouldHide(entity: net.minecraft.world.entity.Entity, distSqr: Double): HideState = shouldHide(entity.bukkitEntity, distSqr)
    abstract fun shouldHide(entity: Entity, distSqr: Double): HideState

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
                    trackedEntities.put(entity.entityId, TrackedEntity(entity))
                }
            }
            listener.register(plugin)
            scheduleTick()
        }
    }

    open fun tick() {
        setupUpdate()
        try {
            update()
        } catch (e: Throwable) {
            plugin.logger.log(Level.SEVERE, "Failed to update ${player.name}", e)
        }
        postUpdate()
    }

    protected open fun setupUpdate() { }

    protected open fun update() {
        val viewDist = player.sendViewDistance + 1 // Add 1 to debounce
        val maxSqrDist = (viewDist shl 4).square() shl 1 // Diagonal

        val playerHandle = HANDLE_GETTER.getHandle(player)
        val level = LEVEL_GETTER.level(playerHandle)
        val pos = playerHandle.position()

        val iterator = trackedEntities.int2ReferenceEntrySet().iterator()
        for (entry in iterator) {
            val te = entry.value
            val bukkit = te.bukkitEntity
            val handle = HANDLE_GETTER.getHandle(bukkit)
            if (!VALID_TESTER.isValid(handle)) {
                // The entity has been removed from the world
                iterator.remove()
                continue
            }
            if (LEVEL_GETTER.level(handle) !== level) {
                // Entity is not on same world anymore
                if (processFarEntity(entry, false)) iterator.remove()
                continue
            }
            val other = handle.position()
            val xzDist = (pos.x - other.x).square() + (pos.z - other.z).square()
            if (xzDist > maxSqrDist) {
                // Entity is out of player visible distance
                if (processFarEntity(entry, true)) iterator.remove()
                continue
            }

            when (shouldHide(handle, xzDist + (pos.y - other.y).square())) {
                HideState.HIDE -> if (!te.hidden) processReverse(te)
                HideState.SHOW -> if (te.hidden) processReverse(te)
                HideState.SKIP -> {}
            }
        }
    }

    protected open fun postUpdate() { }

    protected open fun processReverse(trackedEntity: TrackedEntity) {
        if (!trackedEntity.hidden) {
            player.hideEntity(plugin, trackedEntity.bukkitEntity)
            trackedEntity.hidden = true
        } else {
            player.showEntity(plugin, trackedEntity.bukkitEntity)
            trackedEntity.hidden = false
        }
    }

    protected abstract fun processFarEntity(entry: Int2ReferenceMap.Entry<TrackedEntity>, attemptTrack: Boolean): Boolean

    open fun shutdown() {
        listener.unregister()
        try {
            player.syncTick {
                task?.cancel()
                task = null
                for (te in trackedEntities.values) {
                    if (te.hidden && VALID_TESTER.isValid(te.bukkitEntity)) {
                        VISIBILITY_HANDLER.showEntity(player, te.bukkitEntity, plugin)
                    }
                }
                trackedEntities.clear()
            }
        } catch (e: IllegalStateException) {
            core.err("Failed to restore state for player ${player.name}, offline?", e)
        }
    }

    protected abstract fun scheduleTick()

    data class TrackedEntity(
        @JvmField val bukkitEntity: Entity,
        @JvmField var hidden: Boolean = false,
    )

    inner class TrackListener: Listener {
        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        fun onTrackEntity(e: PlayerTrackEntityEvent) {
            if (e.player !== player) return
            if (e.entity is Player) return // Do not hide players, while this removes them from tab list

            val entityId = e.entity.entityId
            if (trackedEntities.containsKey(entityId)) return
            val hidden = shouldHideDefault(e.entity)
            if (hidden) {
                player.hideEntity(plugin, e.entity)
                e.isCancelled = true
            }
            trackedEntities.put(entityId, TrackedEntity(e.entity, hidden))
        }
    }

    enum class HideState {
        HIDE,
        SHOW,
        SKIP,
    }

    abstract class SyncTick(player: Player, plugin: Plugin) : PlayerEntityVisibilityProcessor(player, plugin) {

        abstract val updateIntervalTicks: Long

        override fun processFarEntity(entry: Int2ReferenceMap.Entry<TrackedEntity>, attemptTrack: Boolean): Boolean {
            val trackedEntity = entry.value
            if (trackedEntity.hidden) {
                if (attemptTrack) {
                    VISIBILITY_HANDLER.showEntity(player, trackedEntity.bukkitEntity, plugin)
                } else {
                    VISIBILITY_HANDLER.forceShowEntity(player, trackedEntity.bukkitEntity, plugin)
                }
            }
            return true
        }

        override fun scheduleTick() {
            task = player.delayedTick(updateIntervalTicks) {
                tick()
                scheduleTick()
            } ?: error("Failed to schedule update task for player ${player.name}")
        }

    }

    abstract class OffTick(player: Player, plugin: Plugin) : PlayerEntityVisibilityProcessor(player, plugin) {

        protected val tickFar = ArrayList<FarEntry>()
        protected val tickReverse = ArrayList<TrackedEntity>()

        override fun processReverse(trackedEntity: TrackedEntity) {
            tickReverse.add(trackedEntity)
        }

        override fun processFarEntity(entry: Int2ReferenceMap.Entry<TrackedEntity>, attemptTrack: Boolean): Boolean {
            tickFar.add(FarEntry(entry.intKey, entry.value))
            return false
        }

        override fun postUpdate() {
            val tickFar = ArrayList(tickFar)
            val tickReverse = ArrayList(tickReverse)
            this.tickFar.clear()
            this.tickReverse.clear()
            task = player.nextTick {
                if (task != null) { // Got shut-down?
                    for (entry in tickFar) {
                        val trackedEntity = entry.trackedEntity
                        if (trackedEntity.hidden) {
                            VISIBILITY_HANDLER.showEntity(player, trackedEntity.bukkitEntity, plugin)
                        }
                        trackedEntities.remove(entry.entityId)
                    }
                    for (trackedEntity in tickReverse) {
                        super.processReverse(trackedEntity)
                    }
                }
            }
        }

        protected class FarEntry(
            @JvmField val entityId: Int,
            @JvmField val trackedEntity: TrackedEntity,
        )

    }

    companion object {
        private val VISIBILITY_HANDLER by Versioned(PlayerEntityVisibilityHandler::class.java)
        private val VALID_TESTER by Versioned(EntityValidTester::class.java)
        private val HANDLE_GETTER by Versioned(EntityHandleGetter::class.java)
        private val LEVEL_GETTER by Versioned(LevelHandler::class.java)
    }

}