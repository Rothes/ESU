package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class UserCullData(
    val player: Player,
) {

    private val hiddenEntities = Int2ReferenceOpenHashMap<Entity>()
    private val pendingChanges = mutableListOf<CulledChange>()
    private var tickedTime = 0

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
        val iterator = hiddenEntities.int2ReferenceEntrySet().iterator()
        for (entry in iterator) {
            val id = entry.intKey
            val entity = entry.value
            iterator.remove()
            pendingChanges.add(CulledChange(entity, id, false))
        }
        updateChanges()
    }

    fun tick() {
        if (++tickedTime >= 60 * 2 * 20) {
            checkEntitiesValid()
        }
        updateChanges()
    }

    private fun updateChanges() {
        val list = pendingChanges.toList()
        if (list.isEmpty()) return
        pendingChanges.clear()
        if (plugin.isEnabled) {
            Scheduler.schedule(player) {
                for (change in list) {
                    if (change.entity.isDead) continue
                    if (!change.entity.checkTickThread()) {
                        // Not on tick thread, we can only save it
                        setCulled(change.entity, change.entityId, change.culled, false)
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

    fun onEntityRemove(entityId: Int) {
        hiddenEntities.remove(entityId)
    }

    fun checkEntitiesValid() {
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

    private fun pendCulledChange(entity: Entity, entityId: Int, culled: Boolean) {
        pendingChanges.add(CulledChange(entity, entityId, culled))
    }

    data class CulledChange(val entity: Entity, val entityId: Int, val culled: Boolean)

}