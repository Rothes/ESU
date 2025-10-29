package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class UserCullData(
    val player: Player,
) {

    private val hiddenEntities = Int2ReferenceOpenHashMap<Entity>()
    private val pendingChanges = mutableListOf<VisibleChange>()

    fun hiddenEntities(): List<Entity> {
        return hiddenEntities.values.toList()
    }

    fun setCulled(entity: Entity, entityId: Int, culled: Boolean) {
        if (culled) {
            if (hiddenEntities.put(entityId, entity) == null)
                pendVisibleChange(entity, false)
        } else {
            if (hiddenEntities.remove(entityId) != null)
                pendVisibleChange(entity, true)
        }
    }

    fun showAll() {
        val iterator = hiddenEntities.iterator()
        for ((_, entity) in iterator) {
            iterator.remove()
            pendingChanges.add(VisibleChange(entity, true))
        }
        update()
    }

    fun update() {
        val list = pendingChanges.toList()
        if (list.isEmpty()) return
        pendingChanges.clear()
        Scheduler.schedule(player) {
            for (change in list) {
                if (change.visible)
                    player.showEntity(bootstrap, change.entity)
                else
                    player.hideEntity(bootstrap, change.entity)
            }
        }
    }

    private fun pendVisibleChange(entity: Entity, visible: Boolean) {
        pendingChanges.add(VisibleChange(entity, visible))
    }

    data class VisibleChange(val entity: Entity, val visible: Boolean)

}