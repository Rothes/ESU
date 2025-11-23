package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.CullDataManager.raytraceHandler
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.connected
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.core.util.extension.forEachInt
import io.github.rothes.esu.core.util.extension.math.square
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class UserCullData(
    var player: Player,
) {

    companion object {
        val playerEntityVisibilityHandler by Versioned(PlayerEntityVisibilityHandler::class.java)
    }

    private val hiddenEntities = Int2ReferenceOpenHashMap<Entity>(64, Hash.VERY_FAST_LOAD_FACTOR)
    private val pendingChanges = mutableListOf<CulledChange>()
    private var tickedTime = 0
    private var isRemoved = false
    private var terminated = false

    var shouldCull = true

    @Synchronized
    fun setCulled(entity: Entity, entityId: Int, culled: Boolean, pend: Boolean = true) {
        if (culled) {
            if (hiddenEntities.put(entityId, entity) == null && pend)
                pendCulledChange(entity, true)
        } else {
            if (hiddenEntities.remove(entityId) != null && pend)
                pendCulledChange(entity, false)
        }
    }

    fun showAll() {
        reset()
        updateChanges()
    }

    fun tick() {
        if (++tickedTime >= 60 * 2 * 20) {
            checkEntitiesValid()
            tickedTime = 0
        }
        updateChanges()
    }

    fun onEntityRemove(entities: IntArrayList) {
        if (entities.isEmpty) return
        synchronized(this) {
            entities.forEachInt { i ->
                hiddenEntities.remove(i)
            }
        }
    }

    fun markRemoved() {
        isRemoved = true
    }

    @Synchronized
    private fun reset() {
        val values = hiddenEntities.values
        hiddenEntities.clear()
        for (entity in values) {
            pendCulledChange(entity, false)
        }
    }

    @Synchronized
    private fun checkEntitiesValid() {
        try {
            val raytraceHandler = raytraceHandler
            val iterator = hiddenEntities.int2ReferenceEntrySet().iterator()
            val playerLoc = player.location
            for (entry in iterator) {
                val entity = entry.value
                var flag = !raytraceHandler.isValid(entity)
                val loc = entity.location
                if (loc.world != playerLoc.world || (playerLoc.x - loc.x).square() + (playerLoc.z - loc.z).square() > 1024 * 1024) {
                    playerEntityVisibilityHandler.forceShowEntity(player, entity)
                    flag = true
                }
                if (flag) iterator.remove()
            }
        } catch (e: Throwable) {
            plugin.err("[EntityCulling] Failed to check entities valid for player ${player.name}", e)
        }
    }

    private fun updateChanges() {
        if (terminated) return

        val changes = synchronized(this) {
            if (pendingChanges.isEmpty()) return
            val temp = pendingChanges.toTypedArray()
            pendingChanges.clear()
            temp
        }
        if (plugin.isEnabled) {
            if (!player.connected)
                Bukkit.getPlayer(player.uniqueId)?.let { player = it }
            Scheduler.schedule(player) {
                val raytraceHandler = raytraceHandler
                for (change in changes) {
                    if (!raytraceHandler.isValid(change.entity)) continue
                    if (!change.entity.checkTickThread()) {
                        // Not on tick thread, we can only reflect to make changes,
                        // We don't need to update TrackedEntity cuz not same thread.
                        if (!change.culled) {
                            playerEntityVisibilityHandler.forceShowEntity(player, change.entity)
                        }
                        continue
                    }
                    if (change.culled)
                        player.hideEntity(bootstrap, change.entity)
                    else
                        player.showEntity(bootstrap, change.entity)
                }
            } ?: let {
                plugin.warn("[EntityCulling] Failed to schedule changes ${player.name}, not online?")
            }
        }
    }

    private fun pendCulledChange(entity: Entity, culled: Boolean) {
        pendingChanges.add(CulledChange(entity, culled))
    }

    private data class CulledChange(val entity: Entity, val culled: Boolean)

}