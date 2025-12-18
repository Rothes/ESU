package io.github.rothes.esu.bukkit.listener

import io.github.rothes.esu.bukkit.UpdateCheckerMan
import io.github.rothes.esu.bukkit.event.*
import io.github.rothes.esu.bukkit.inventory.EsuInvHolder
import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelHandler
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.extension.math.square
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import net.minecraft.server.level.ServerLevel
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import kotlin.math.abs

internal object InternalListeners : Listener {

    private val emoteCommands = RawUserEmoteEvent.EMOTE_COMMANDS.split('|').toSet()
    private val whisperCommands = RawUserWhisperEvent.WHISPER_COMMANDS.split('|').toSet()
    private val replyCommands = RawUserReplyEvent.REPLY_COMMANDS.split('|').toSet()

    init {
        register()

        // Init event listeners
        Dynamic.userTrackEntityListeners.register()
        UserLoginEvent.Companion
    }

    /* User data loading */

    @EventHandler(priority = EventPriority.MONITOR)
    fun onLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult == AsyncPlayerPreLoginEvent.Result.ALLOWED)
            BukkitUserManager[event.uniqueId]
        else
            BukkitUserManager.unload(event.uniqueId)
    }
    @EventHandler(priority = EventPriority.LOWEST)
    fun onLogin(event: PlayerJoinEvent) {
        val user = BukkitUserManager[event.player]
        UpdateCheckerMan.onJoin(user)
    }
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        BukkitUserManager.getCache(event.player.uniqueId)?.let {
            StorageManager.updateUserAsync(it)
            BukkitUserManager.unload(it)
        }
    }

    /* Rich message events */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        RichPlayerDeathEvent(event).callEvent()
    }

    /* EsuInvHolder processing */

    @EventHandler(priority = EventPriority.LOWEST)
    fun onClick(e: InventoryClickEvent) {
        val holder = e.inventory.holder
        if (holder is EsuInvHolder<*>) {
            holder.handleClick(e)
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    fun onClick(e: InventoryDragEvent) {
        val holder = e.inventory.holder
        if (holder is EsuInvHolder<*>) {
            holder.handleDrag(e)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onClick(e: InventoryCloseEvent) {
        val holder = e.inventory.holder
        if (holder is EsuInvHolder<*>) {
            holder.onClose()
        }
    }

    /* Esu nested events processing */

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChatCommand0(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.LOWEST)
    }
    @EventHandler(priority = EventPriority.LOW)
    fun onChatCommand1(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.LOW)
    }
    @EventHandler(priority = EventPriority.NORMAL)
    fun onChatCommand2(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.NORMAL)
    }
    @EventHandler(priority = EventPriority.HIGH)
    fun onChatCommand3(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.HIGH)
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onChatCommand4(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.HIGHEST)
    }

    private fun onChatCommand(e: PlayerCommandPreprocessEvent, priority: EventPriority) {
        val message = e.message
        val split = message.split(' ', limit = 3)
        val command = split[0].substring(1).split(':').last().lowercase()
        if (emoteCommands.contains(command)) {
            if (split.size >= 2 && RawUserEmoteEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = RawUserEmoteEvent(
                    e.player,
                    split.drop(1).joinToString(separator = " "),
                    e.isCancelled,
                    priority,
                )
                event.callNested()
                e.isCancelled = event.isCancelled
            }
        } else if (whisperCommands.contains(command)) {
            if (split.size >= 3 && RawUserWhisperEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = RawUserWhisperEvent(
                    e.player,
                    split[1],
                    split[2],
                    e.isCancelled,
                    priority,
                )
                event.callNested()
                e.isCancelled = event.isCancelled
            }
        } else if (replyCommands.contains(command)) {
            if (split.size >= 2 && RawUserReplyEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = RawUserReplyEvent(
                    e.player,
                    split.drop(1).joinToString(separator = " "),
                    e.isCancelled,
                    priority,
                )
                event.callNested()
                e.isCancelled = event.isCancelled
            }
        }
    }

    private object Dynamic {
        val userTrackEntityListeners = if (UserTrackEntityEvent.FULL_SUPPORT) {
            object : Listener {
                // The only one uses EventPriority.HIGHEST while we are modifying the result
                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
                fun onTrackEntity(e: PlayerTrackEntityEvent) {
                    if (UserTrackEntityEvent.getHandlerList().registeredListeners.isEmpty()) return
                    if (!UserTrackEntityEvent(e.player, e.entity).callEvent())
                        e.isCancelled = true
                }
            }
        } else {
            object : Listener {

                private val levelHandler by Versioned(LevelHandler::class.java)

                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                fun onPossibleEntityTrack(event: EntitySpawnEvent) {
                    if (UserTrackEntityEvent.getHandlerList().registeredListeners.isEmpty()) return

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
                    if (UserTrackEntityEvent.getHandlerList().registeredListeners.isEmpty()) return

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
    }

}