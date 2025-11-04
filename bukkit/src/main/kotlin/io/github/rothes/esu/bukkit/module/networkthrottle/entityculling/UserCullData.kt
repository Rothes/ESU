package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class UserCullData(
    val player: Player,
) {

    private val hiddenEntities = Int2ReferenceOpenHashMap<Entity>(32, Hash.FAST_LOAD_FACTOR)
    private val pendingChanges = mutableListOf<CulledChange>()
    private var tickedTime = 0
    private var isRemoved = false
    private var terminated = false

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
                val iterator = hiddenEntities.int2ReferenceEntrySet().iterator()
                for (entry in iterator) {
                    val entity = entry.value
                    if (entity.isDead) {
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
            Scheduler.schedule(player) {
                for (change in list) {
                    if (change.entity.isDead) continue
                    if (!change.entity.checkTickThread()) {
                        // Not on tick thread, we can only roll state back
                        if (!change.culled)
                            setCulled(change.entity, change.entityId, true, pend = false)
                        continue
                    }
                    // This has to be checked on tick thread on 1.20.6-1.21.1 (or maybe some other version)
                    // because they do this.isInWorld() check which uses getHandle()
                    if (!change.entity.isValid) continue
                    if (change.culled)
                        player.hideEntity(bootstrap, change.entity)
                    else
                        player.showEntity(bootstrap, change.entity)
                }
            }
        }
    }

    private fun pendCulledChange(entity: Entity, entityId: Int, culled: Boolean) {
        pendingChanges.add(CulledChange(entity, entityId, culled))
    }

    private data class CulledChange(val entity: Entity, val entityId: Int, val culled: Boolean)

}