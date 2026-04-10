package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.nextTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.connected
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryView
import org.incendo.cloud.annotations.Command
import java.util.concurrent.ConcurrentHashMap

object EnderChest: BaseCommand<FeatureToggle.DefaultTrue, Unit>() {

    private val viewMap = ConcurrentHashMap<InventoryView, Boolean>() // View -> AllowEdit

    override fun onEnable() {
        Listeners.register()
        registerCommands(object {
            @Command("esuExperimental openEnderChest|enderChest")
            @ShortPerm
            fun openEnderChest(sender: PlayerUser) {
                openEnderChest(sender, sender.player)
            }

            @Command("esuExperimental openEnderChest|enderChest <player>")
            @ShortPerm("others")
            fun openEnderChest(sender: PlayerUser, player: Player) {
                val senderPlayer = sender.player
                senderPlayer.syncTick {
                    val enderChest = player.enderChest
                    val openInventory = senderPlayer.openInventory(enderChest) ?: return@syncTick
                    val editPerm = cmdShortPerm(if (player == senderPlayer) "edit" else "others.edit")
                    viewMap[openInventory] = sender.hasPermission(editPerm)

                    fun checkAvailable() {
                        senderPlayer.nextTick {
                            if (senderPlayer.openInventory != openInventory) {
                                return@nextTick
                            }

                            if (!player.connected) {
                                senderPlayer.closeInventory()
                                viewMap.remove(openInventory)
                            }
                            checkAvailable()
                        }
                    }
                    checkAvailable()
                }
            }

        }) { parser ->
            parser.registerBuilderDecorator {
                it.senderType(PlayerUser::class.java)
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        for (view in viewMap.keys) {
            view.close()
        }
        viewMap.clear()
        Listeners.unregister()
    }

//    private class EnderChestInv(
//        val viewer: PlayerUser,
//        val target: Player,
//    ): EsuInvHolder<Unit>(InternalInvData) {
//
//        private val modifiedSlots = Int2ReferenceOpenHashMap<ItemStack?>() // Slot -> Original Item
//        private val enderChest = target.enderChest
//        private var task: ScheduledTask?
//        private val lock = ReentrantLock()
//
//        init {
//            update()
//
//            task = Scheduler.schedule(target, 1, 1) {
//                lock.withLock {
//                    // Apply edits
//                    for (entry in modifiedSlots.int2ReferenceEntrySet()) {
//                        fun rollbackTransaction(item: ItemStack?) {
//                            item?.let { viewer.player.inventory.addItem(item) }
//                        }
//
//                        val new = inventory.getItem(entry.intKey)
//                        val slot = entry.intKey
//                        val originalItem = entry.value
//
//                        if (enderChest.getItem(slot) == originalItem) {
//                            enderChest.setItem(slot, new)
//                        } else {
//                            rollbackTransaction(new)
//                        }
//                    }
//                    modifiedSlots.clear()
//
//                    if (target.connected) update() else close()
//                }
//            }
//        }
//
//        override fun onClose() {
//            task?.cancel()
//            task = null
//        }
//
//        private fun update() {
//            inventory.contents = enderChest.contents
//        }
//
//        private object InternalInvData: InventoryData<Unit>(size = 3 * 9)
//    }

    private object Listeners: Listener {

        @EventHandler
        fun onClick(e: InventoryClickEvent) {
            val allowEdit = viewMap[e.view] ?: return
            if (!allowEdit)
                e.isCancelled = true
        }

        @EventHandler
        fun onDrag(e: InventoryDragEvent) {
            val allowEdit = viewMap[e.view] ?: return
            if (!allowEdit)
                e.isCancelled = true
        }

        @EventHandler
        fun onClose(e: InventoryCloseEvent) {
            viewMap.remove(e.view)
        }

        @EventHandler
        fun onQuit(e: PlayerQuitEvent) {
            viewMap.remove(e.player.openInventory)
        }

    }

}