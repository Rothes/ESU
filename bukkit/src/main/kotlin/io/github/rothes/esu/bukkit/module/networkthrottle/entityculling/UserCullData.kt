package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.util.entity.MapPlayerEntityVisibilityHolder
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.nextTick
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.connected
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityValidTester
import it.unimi.dsi.fastutil.Hash
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.locks.ReentrantLock

class UserCullData(
    var player: Player,
) {

    companion object {
        private val VISIBILITY_HANDLER by Versioned(PlayerEntityVisibilityHandler::class.java)
        private val VALID_TESTER by Versioned(EntityValidTester::class.java)
    }

    @ApiStatus.Internal val lock = ReentrantLock()
    private val hiddenHolder = MapPlayerEntityVisibilityHolder(player, bootstrap, 64, Hash.FAST_LOAD_FACTOR)
    private val pendingChanges = mutableListOf<CulledChange>()
    private var tickedTime = 0
    private var isRemoved = false
    private var terminated = false

    var shouldCull = true

    fun setCulled(entity: Entity, entityId: Int, culled: Boolean, pend: Boolean = true) {
        if (culled) {
            if (hiddenHolder.map.put(entityId, entity) == null && pend)
                pendCulledChange(entity, true)
        } else {
            if (hiddenHolder.map.remove(entityId) != null && pend)
                pendCulledChange(entity, false)
        }
    }

    fun showAll() {
        withLock {
            reset()
            updateChanges()
        }
    }

    fun tick() {
        if (++tickedTime >= 60 * 2 * 20) {
            checkEntitiesValid()
            tickedTime = 0
        }
        updateChanges()
    }

    fun onEntityRemove(entities: IntArray) {
        if (entities.isEmpty()) return
        for (i in entities) {
            hiddenHolder.map.remove(i)
        }
    }

    fun markRemoved() {
        isRemoved = true
    }

    inline fun <T> withLock(action: () -> T): T {
        lock.lock()
        try {
            return action()
        } finally {
            lock.unlock()
        }
    }

    private fun reset() {
        for (entity in hiddenHolder.map.values) {
            pendCulledChange(entity, false)
        }
        hiddenHolder.map.clear()
    }

    private fun checkEntitiesValid() {
        hiddenHolder.checkEntitiesValid()
    }

    private fun updateChanges() {
        if (terminated || pendingChanges.isEmpty()) return

        val changes = pendingChanges.toTypedArray()
        pendingChanges.clear()

        if (!player.connected) Bukkit.getPlayer(player.uniqueId)?.let { player = it }
        player.nextTick {
            for (change in changes) {
                if (!VALID_TESTER.isValid(change.entity)) continue
                if (!change.entity.checkTickThread()) {
                    // Not on tick thread, we can only reflect to make changes,
                    // We don't need to update TrackedEntity cuz not same thread.
                    if (!change.culled) {
                        VISIBILITY_HANDLER.forceShowEntity(player, change.entity)
                    }
                    continue
                }
                if (change.culled) {
                    // hideEntity requires plugin enabled
                    if (bootstrap.isEnabled) player.hideEntity(bootstrap, change.entity)
                } else {
                    player.showEntity(bootstrap, change.entity)
                }
            }
        }
    }

    private fun pendCulledChange(entity: Entity, culled: Boolean) {
        pendingChanges.add(CulledChange(entity, culled))
    }

    private data class CulledChange(val entity: Entity, val culled: Boolean)

}