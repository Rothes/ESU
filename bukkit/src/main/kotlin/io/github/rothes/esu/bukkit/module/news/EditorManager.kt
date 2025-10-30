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
import io.github.rothes.esu.bukkit.module.NewsModule
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.meta
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.metaGet
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.BookMeta

object EditorManager {

    private val editing = mutableMapOf<Player, EditData>()
    private val confirming = mutableMapOf<Player, ConfirmData>()

    fun enable() {
        Listeners.register()
        PacketEvents.getAPI().eventManager.registerListener(PacketListeners)
    }

    fun disable() {
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListeners)
        Listeners.unregister()
        editing.clear()
        confirming.clear()
    }

    fun getEditing(player: Player): EditData? {
        return editing[player]
    }

    fun startEdit(user: PlayerUser, content: String, newsId: Int, lang: String,
                  cancel: () -> Unit, complete: (EditorResult) -> Unit, callCancel: Boolean = false) {
        startEdit(user, listOf(content), newsId, lang, cancel, complete, callCancel)
    }

    fun startEdit(user: PlayerUser, content: List<String>, newsId: Int, lang: String,
                  cancel: () -> Unit, complete: (EditorResult) -> Unit, callCancel: Boolean = false) {
        val item = user.item(NewsModule.lang, { bookNews.editor.editItem.copy(material = Material.WRITABLE_BOOK) })
        val player = user.player

        cancelEdit(player, callCancel)
        val slot = player.inventory.heldItemSlot
        editing[player] = EditData(slot, newsId, lang, cancel, complete)

        item.meta { meta: BookMeta ->
            meta.pages = content
        }
        val packet = WrapperPlayServerSetSlot(
            -2, 0, slot, SpigotConversionUtil.fromBukkitItemStack(item)
        )
        PacketEvents.getAPI().playerManager.sendPacket(user.player, packet)
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
        data.complete(EditorResult(data, content))
    }

    fun toConfirm(player: Player, data: EditorResult, block: () -> Unit) {
        confirming[player] = ConfirmData(data, block)
    }

    fun confirm(player: Player): Boolean {
        confirming.remove(player)?.block?.invoke() ?: return false
        return true
    }

    fun cancel(player: Player): Boolean {
        confirming.remove(player) ?: return false
        return true
    }

    fun editAgain(player: Player) {
        val result = confirming.remove(player)?.editorResult ?: return
        val (_, newsId, lang, cancel, complete) = result.editData
        startEdit(player.user, result.content, newsId, lang, cancel, complete)
    }

    private fun restoreSlot(player: Player, slot: Int) {
        val item = player.inventory.getItem(slot)
        val packet = WrapperPlayServerSetSlot(
            -2, 0, slot, SpigotConversionUtil.fromBukkitItemStack(item)
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
                    val pages = wrapper.pages
                        ?: SpigotConversionUtil.toBukkitItemStack(wrapper.itemStack).metaGet { meta: BookMeta ->
                            meta.pages
                        }
                    completeEdit(player, pages)
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
        val editData: EditData,
        val content: List<String>,
    )

    private data class ConfirmData(
        val editorResult: EditorResult,
        val block: () -> Unit,
    )
}