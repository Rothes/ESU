package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.CullDataManager.raytraceHandler
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.connected
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class UserCullData(
    var player: Player,
) {

    private val hiddenEntities = Int2ReferenceOpenHashMap<Entity>(64, Hash.VERY_FAST_LOAD_FACTOR)
    private val pendingChanges = mutableListOf<CulledChange>()
    private var tickedTime = 0
    private var isRemoved = false
    private var terminated = false

    var shouldCull = true

    fun setCulled(entity: Entity, entityId: Int, culled: Boolean, pend: Boolean = true) {
        if (culled) {
            synchronized(hiddenEntities) {
                if (hiddenEntities.put(entityId, entity) == null && pend)
                    pendCulledChange(entity, entityId, true)
            }
        } else {
            synchronized(hiddenEntities) {
                if (hiddenEntities.remove(entityId) != null && pend)
                    pendCulledChange(entity, entityId, false)
            }
        }
    }

    fun showAll() {
        reset()
        updateChanges()
    }

    fun tick() {
        if (++tickedTime >= 60 * 2 * 20) {
            checkEntitiesValid()
        }
        updateChanges()
    }

    fun onEntityRemove(entities: IntArrayList) {
        if (entities.isEmpty) return
        val iterator = entities.listIterator()
        synchronized(hiddenEntities) {
            while (iterator.hasNext()) {
                hiddenEntities.remove(iterator.nextInt())
            }
        }
    }

    fun markRemoved() {
        isRemoved = true
    }

    private fun reset() {
        synchronized(hiddenEntities) {
            val iterator = hiddenEntities.int2ReferenceEntrySet().iterator()
            for (entry in iterator) {
                val id = entry.intKey
                val entity = entry.value
                iterator.remove()
                pendCulledChange(entity, id, false)
            }
        }
    }

    private fun checkEntitiesValid() {
        try {
            synchronized(hiddenEntities) {
                val raytraceHandler = raytraceHandler
                val iterator = hiddenEntities.int2ReferenceEntrySet().iterator()
                for (entry in iterator) {
                    val entity = entry.value
                    if (!raytraceHandler.isValid(entity)) {
                        iterator.remove()
                    }
                }
            }
        } catch (e: Throwable) {
            plugin.err("[EntityCulling] Failed to check entities valid for player ${player.name}", e)
        }
    }

    private fun updateChanges() {
        if (terminated) return
        if (pendingChanges.isEmpty()) return

        val list = pendingChanges.toList()
        pendingChanges.clear()
        if (plugin.isEnabled) {
            if (!player.connected)
                Bukkit.getPlayer(player.uniqueId)?.let { player = it }
            Scheduler.schedule(player) {
                val raytraceHandler = raytraceHandler
                for (change in list) {
                    if (!raytraceHandler.isValid(change.entity)) continue
                    if (!change.entity.checkTickThread()) {
                        // Not on tick thread, we can only roll state back
                        if (!change.culled)
                            setCulled(change.entity, change.entityId, true, pend = false)
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

    private fun pendCulledChange(entity: Entity, entityId: Int, culled: Boolean) {
        pendingChanges.add(CulledChange(entity, entityId, culled))
    }

    private data class CulledChange(val entity: Entity, val entityId: Int, val culled: Boolean)

}