package io.github.rothes.esu.bukkit.module.news

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEditBook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.WritableBookMeta

object EditorManager {

    private val editing = mutableMapOf<Player, EditData>()
    private val confirming = mutableMapOf<Player, () -> Unit>()

    fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
        PacketEvents.getAPI().eventManager.registerListener(PacketListeners)
    }

    fun disable() {
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListeners)
        HandlerList.unregisterAll(Listeners)
    }

    fun getEditing(player: Player): EditData? {
        return editing[player]
    }

    fun startEdit(player: Player, content: String, newsId: Int, lang: String, item: ItemStack,
                  cancel: () -> Unit, complete: (EditorResult) -> Unit, callCancel: Boolean = false) {
        startEdit(player, listOf(content), newsId, lang, item, cancel, complete, callCancel)
    }

    fun startEdit(player: Player, content: List<String>, newsId: Int, lang: String, item: ItemStack,
                  cancel: () -> Unit, complete: (EditorResult) -> Unit, callCancel: Boolean = false) {
        cancelEdit(player, callCancel)
        val slot = player.inventory.heldItemSlot
        editing[player] = EditData(slot, newsId, lang, cancel, complete)

        item.editMeta { meta ->
            meta as WritableBookMeta
            meta.pages = content
        }
        val packet = WrapperPlayServerSetSlot(
            -2, 0, slot,
            SpigotConversionUtil.fromBukkitItemStack(item)
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    fun cancelEdit(player: Player, callCancel: Boolean) {
        val data = editing.remove(player) ?: return
        restoreSlot(player, data.slot)
        if (callCancel)
            data.cancel()
    }

    fun completeEdit(player: Player, content: List<String>) {
        val data = editing.remove(player) ?: return
        restoreSlot(player, data.slot)
        data.complete(EditorResult(data.newsId, data.lang, content, data.time))
    }

    fun toConfirm(player: Player, block: () -> Unit) {
        confirming[player] = block
    }

    fun confirm(player: Player): Boolean {
        confirming.remove(player)?.invoke() ?: return false
        return true
    }

    fun cancel(player: Player): Boolean {
        confirming.remove(player) ?: return false
        return true
    }

    private fun restoreSlot(player: Player, slot: Int) {
        val item = player.inventory.getItem(slot)
        val packet = WrapperPlayServerSetSlot(
            -2, 0, slot,
            SpigotConversionUtil.fromBukkitItemStack(item)
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private object PacketListeners: PacketListenerAbstract(PacketListenerPriority.LOWEST) {
        override fun onPacketReceive(event: PacketReceiveEvent) {
            when (event.packetType) {
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.ANIMATION -> {
                    val player = event.getPlayer<Player>()
                    val data = editing[player] ?: return
                    // This will check both main-hand and offhand,
                    // But we don't want to init wrappers so it's fine.
                    val slot = player.inventory.heldItemSlot
                    if (data.slot != slot) return
                    if (data.slotChanged) {
                        cancelEdit(player, true)
                    }
                }
                PacketType.Play.Client.EDIT_BOOK -> {
                    val player = event.getPlayer<Player>()
                    val data = editing[player] ?: return

                    val wrapper = WrapperPlayClientEditBook(event)
                    if (wrapper.slot != data.slot) return
                    completeEdit(player, wrapper.pages)
                    event.isCancelled = true
                }
            }
        }

        override fun onPacketSend(event: PacketSendEvent) {
            when (event.packetType) {
                PacketType.Play.Server.WINDOW_ITEMS -> {
                    val player = event.getPlayer<Player>()
                    editing[player]?.slotChanged = true
                }
                PacketType.Play.Server.SET_SLOT -> {
                    val player = event.getPlayer<Player>()
                    val data = editing[player] ?: return
                    val wrapper = WrapperPlayServerSetSlot(event)
                    // Minus 36 to unify with Bukkit. We set slotChanged on window_items packets,
                    // so we only consider player inventory window.
                    if (wrapper.slot - 36 == data.slot)
                        data.slotChanged = true
                }
            }
        }
    }

    private object Listeners: Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            editing.remove(event.player)
            confirming.remove(event.player)
        }
    }

    data class EditData(
        val slot: Int,
        val newsId: Int,
        val lang: String,
        val cancel: () -> Unit,
        val complete: (EditorResult) -> Unit,
        val time: Long = System.currentTimeMillis(),
        var slotChanged: Boolean = false,
    )

    data class EditorResult(
        val newsId: Int,
        val lang: String,
        val content: List<String>,
        val time: Long,
    )
}