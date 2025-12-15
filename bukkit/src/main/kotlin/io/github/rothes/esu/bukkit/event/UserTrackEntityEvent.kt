package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelHandler
import io.github.rothes.esu.core.util.extension.math.square
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import net.minecraft.server.level.ServerLevel
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.world.ChunkLoadEvent
import kotlin.math.abs

class UserTrackEntityEvent(
    player: Player,
    val entity: Entity
): PlayerEvent(player, false), CancellableKt {

    val user by lazy(LazyThreadSafetyMode.NONE) { player.user }

    override var cancelledKt: Boolean = false

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {

        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

        init {
            val listener = if (ServerCompatibility.isPaper && ServerCompatibility.serverVersion >= 19) {
                object : Listener {
                    // The only one uses EventPriority.HIGHEST while we are modifying the result
                    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
                    fun onTrackEntity(e: PlayerTrackEntityEvent) {
                        if (handlers.registeredListeners.isEmpty()) return
                        if (!UserTrackEntityEvent(e.player, e.entity).callEvent())
                            e.isCancelled = true
                    }
                }
            } else {
                object : Listener {

                    private val levelHandler by Versioned(LevelHandler::class.java)

                    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                    fun onPossibleEntityTrack(event: EntitySpawnEvent) {
                        if (handlers.registeredListeners.isEmpty()) return

                        val entity = (event.entity as CraftEntity).handle
                        val level = levelHandler.level(entity) as? ServerLevel ?: return
                        for (player in level.players()) {
                            val bukkit = player.bukkitEntity
                            val viewDistanceSquared = bukkit.viewDistance.square() shl 8
                            if (entity === player) continue
                            val dist = (player.x - entity.x).square() + (player.z - entity.z).square()
                            if (dist > viewDistanceSquared) continue

                            UserTrackEntityEvent(bukkit, event.entity).callEvent()
                        }
                    }

                    @EventHandler(priority = EventPriority.MONITOR)
                    fun onPossibleEntityTrack(event: ChunkLoadEvent) {
                        if (handlers.registeredListeners.isEmpty()) return

                        for (player in event.world.players) {
                            if (!player.checkTickThread()) continue
                            val viewDistance = player.viewDistance + 2 // Add 2 extra chunks to collect
                            val playerChunk = player.chunk

                            if (abs(event.chunk.x - playerChunk.x) > viewDistance || abs(event.chunk.z - playerChunk.z) > viewDistance)
                                continue

                            for (entity in event.chunk.entities) {
                                UserTrackEntityEvent(player, entity).callEvent()
                            }
                        }
                    }
                }
            }
            listener.register()
        }
    }

}