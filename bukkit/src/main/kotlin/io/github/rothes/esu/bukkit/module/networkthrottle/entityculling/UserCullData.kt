package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class UserCullData(
    val player: Player,
) {

    private val hiddenEntities = Int2ReferenceOpenHashMap<Entity>()
    private val pendingChanges = mutableListOf<CulledChange>()
    private var tickedTime = 0
    private var markedReset = false
    private var isRemoved = false
    private var terminated = false

    fun hiddenEntities(): List<Entity> {
        return hiddenEntities.values.toList()
    }

    fun setCulled(entity: Entity, entityId: Int, culled: Boolean, pend: Boolean = true) {
        if (culled) {
            if (hiddenEntities.put(entityId, entity) == null && pend)
                pendCulledChange(entity, entityId, true)
        } else {
            if (hiddenEntities.remove(entityId) != null && pend)
                pendCulledChange(entity, entityId, false)
        }
    }

    fun showAll() {
        if (isRemoved) {
            reset()
            updateChanges()
            terminated = true
        } else {
            markedReset = true
        }
    }

    fun tick() {
        if (++tickedTime >= 60 * 2 * 20) {
            checkEntitiesValid()
        }
        if (markedReset)
            reset()
        updateChanges()
    }

    fun onEntityRemove(entities: IntArrayList) {
        if (entities.isEmpty) return
        val iterator = entities.listIterator()
        while (iterator.hasNext()) {
            hiddenEntities.remove(iterator.nextInt())
        }
    }

    fun markRemoved() {
        isRemoved = true
    }

    private fun reset() {
        val iterator = hiddenEntities.int2ReferenceEntrySet().iterator()
        for (entry in iterator) {
            val id = entry.intKey
            val entity = entry.value
            iterator.remove()
            pendingChanges.add(CulledChange(entity, id, false))
        }
    }

    private fun checkEntitiesValid() {
        try {
            val iterator = hiddenEntities.int2ReferenceEntrySet().iterator()
            for (entry in iterator) {
                val entity = entry.value
                if (entity.isDead) {
                    iterator.remove()
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
                        setCulled(change.entity, change.entityId, !change.culled, false)
                        continue
                    }
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