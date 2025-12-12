package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.PlayerEntityVisibilityHandler
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityValidTester
import io.github.rothes.esu.core.util.extension.math.square
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.logging.Level

class PlayerEntityVisibilityHolder(
    val player: Player,
    val plugin: Plugin,
    expected: Int = 16,
    factor: Float = 0.75f,
) {

    val map = Int2ReferenceOpenHashMap<Entity>(expected, factor)

    fun checkEntitiesValid() {
        try {
            val iterator = map.int2ReferenceEntrySet().iterator()
            val playerLoc = player.location
            for (entry in iterator) {
                val entity = entry.value
                var flag = !VALID_TESTER.isValid(entity)
                if (!flag) {
                    val loc = entity.location
                    if (loc.world != playerLoc.world || (playerLoc.x - loc.x).square() + (playerLoc.z - loc.z).square() > 1024 * 1024) {
                        // We can just remove hideEntity ticket, it's not necessary to update player
                        // to track that entity cuz they are far away.
                        VISIBILITY_HANDLER.forceShowEntity(player, entity)
                        flag = true
                    }
                }
                if (flag) iterator.remove()
            }
        } catch (e: Throwable) {
            plugin.logger.log(Level.SEVERE, "Failed to check entities valid for player ${player.name}", e)
        }
    }

    fun showAll() {
        val entities = map.values.toList()
        map.clear()
        player.syncTick(plugin) {
            for (entity in entities) {
                if (VALID_TESTER.isValid(entity)) {
                    if (entity.checkTickThread())
                        player.showEntity(plugin, entity)
                    else
                        VISIBILITY_HANDLER.forceShowEntity(player, entity, plugin)
                }
            }
        }
    }


    companion object {
        private val VISIBILITY_HANDLER by Versioned(PlayerEntityVisibilityHandler::class.java)
        private val VALID_TESTER by Versioned(EntityValidTester::class.java)
    }

}