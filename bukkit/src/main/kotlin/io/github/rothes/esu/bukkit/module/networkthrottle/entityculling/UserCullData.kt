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
        update()
    }

    fun update() {
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
        val iterator = hiddenEntities.int2ReferenceEntrySet().iterator()
        for (entry in iterator) {
            val entity = entry.value
            if (entity.isDead) {
                plugin.warn("[EntityCulling] Found an removed entity ${entry.intKey} for player ${player.name}")
                iterator.remove()
            }
        }
    }

    private fun pendCulledChange(entity: Entity, entityId: Int, culled: Boolean) {
        pendingChanges.add(CulledChange(entity, entityId, culled))
    }

    data class CulledChange(val entity: Entity, val entityId: Int, val culled: Boolean)

}