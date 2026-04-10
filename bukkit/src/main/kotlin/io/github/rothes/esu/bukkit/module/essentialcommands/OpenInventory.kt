package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.configuration.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import io.github.rothes.esu.bukkit.inventory.type.SimpleType
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.onTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.InventoryAdapter.Companion.topInv
import io.github.rothes.esu.bukkit.util.version.adapter.nms.PlayerInventoryViewGetter
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArraySet
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotations.Command
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object OpenInventory : BaseCommand<FeatureToggle.DefaultTrue, OpenInventory.Lang>() {

    private lateinit var invConfig: PlayerInventoryData

    override fun onReload() {
        loadInvConfig()
        super.onReload()
    }

    override fun onEnable() {
        registerCommands(object {
            @Command("esuExperimental openInventory|openInv <player>")
            @ShortPerm
            fun openInventory(sender: PlayerUser, player: Player) {
                PlayerInventory(sender, player).open(sender)
                player.enderChest
            }

        }) { parser ->
            parser.registerBuilderDecorator {
                it.senderType(PlayerUser::class.java)
            }
        }
    }

    private fun loadInvConfig() {
        val file = module.moduleFolder.resolve("inventory").resolve("$name.yml")
        invConfig = ConfigLoader.load(file)
    }

    class PlayerInventoryData: InventoryData<Unit>(
        size = 6 * 9,
        layout = """
            EEEEEDDCC
            QDDDDDDCC
            IIIIIIIII
            IIIIIIIII
            IIIIIIIII
            HHHHHHHHH
        """.trimIndent(),
        icons = mapOf(
            'C' to InventoryItem(type = "Craft-Slot"),
            'H' to InventoryItem(type = "HotBar-Slot"),
            'E' to InventoryItem(type = "Equipment-Slot"),
            'I' to InventoryItem(type = "Inventory-Slot"),
            'Q' to InventoryItem(type = "Cursor"),
            'D' to InventoryItem(type = "Drop"),
        )
    )

    data class Lang(
        val playerInventoryTitle: String = "<shadow:black:0.11><sdc><player><sc>'s Inventory"
    )

    class PlayerInventory(
        val viewer: PlayerUser,
        val target: Player,
    ) : DynamicHolder<Unit>(invConfig) {

        private var cursorSlot: Int? = null
        private val slotPairs = mutableListOf<SlotPair>()
        private val craftSlotPairs = mutableListOf<SlotPair>()
        private val dropSlots = IntArraySet()
        private val targetInv = target.inventory
        private val craftInv = INVENTORY_VIEW_GETTER.getCraftingInventoryView(target).topInv as CraftingInventory
        private val allowEdit = viewer.hasPermission(cmdShortPerm("edit"))
        private val modifiedSlots = Int2ReferenceOpenHashMap<PendingChange>()

        private val lock = ReentrantLock()
        private var task: ScheduledTask?

        init {
            var craftSlot = 1 // Slot 0 is the result
            var hotBarSlot = 0
            var invSlot = 9
            var equipmentSlot = 0


            fun listenInvClick(slot: Int, targetSlot: Int, inv: Inventory) {
                if (allowEdit)
                    click(slot) { e ->
                        modifiedSlots.computeIfAbsent(slot) {
                            lock.withLock {
                                PendingChange(inv, targetSlot, inv.getItem(targetSlot))
                            }
                        }
                        e.isCancelled = false
                    }
            }

            typeRegistry.register(
                SimpleType.create("Craft-Slot") { slot, _ ->
                    val targetSlot = craftSlot++
                    if (targetSlot == 5)
                        return@create null

                    listenInvClick(slot, targetSlot, craftInv)
                    craftSlotPairs.add(SlotPair(targetSlot, slot))
                    craftInv.getItem(targetSlot)
                },
                SimpleType.create("HotBar-Slot") { slot, _ ->
                    val targetSlot = hotBarSlot++
                    if (targetSlot == 9)
                        return@create null

                    listenInvClick(slot, targetSlot, targetInv)
                    slotPairs.add(SlotPair(targetSlot, slot))
                    targetInv.getItem(targetSlot)
                },
                SimpleType.create("Inventory-Slot") { slot, _ ->
                    val targetSlot = invSlot++
                    if (targetSlot == 36) {
                        return@create null
                    }

                    listenInvClick(slot, targetSlot, targetInv)
                    slotPairs.add(SlotPair(targetSlot, slot))
                    targetInv.getItem(targetSlot)
                },
                SimpleType.create("Equipment-Slot") { slot, _ ->
                    val targetSlot = when (equipmentSlot++) {
                        0 -> 36 + 3
                        1 -> 36 + 2
                        2 -> 36 + 1
                        3 -> 36 + 0
                        4 -> 36 + 4
                        else -> return@create null
                    }

                    listenInvClick(slot, targetSlot, targetInv)
                    slotPairs.add(SlotPair(targetSlot, slot))
                    targetInv.getItem(targetSlot)
                },
                SimpleType.create("Drop") { slot, _ ->
                    if (allowEdit) {
                        click(slot) { e ->
                            val item = e.cursor
                            target.syncTick {
                                target.dropItem(item)
                            }
                            e.view.setCursor(null)
                        }
                        dropSlots.add(slot)
                    }
                    null
                },
                SimpleType.create("Cursor") { slot, _ ->
                    cursorSlot = slot
                    val targetView = target.openInventory
                    if (allowEdit)
                        click(slot) { e ->
                            lock.withLock {
                                modifiedSlots.computeIfAbsent(slot) {
                                    PendingChange(targetInv, -1, targetView.cursor)
                                }
                            }
                            e.isCancelled = false
                        }
                    targetView.cursor
                },
            )

            setTitle(viewer.buildMiniMessage(lang, { playerInventoryTitle }, player(target)))
            task = Scheduler.schedule(target, 1, 1) {
                lock.withLock {
                    // Apply edits
                    for (entry in modifiedSlots.int2ReferenceEntrySet()) {
                        fun rollbackTransaction(item: ItemStack?) {
                            item?.let { viewer.player.inventory.addItem(item) }
                        }

                        val new = inventory.getItem(entry.intKey)
                        val (inv, targetSlot, originalItem) = entry.value
                        if (targetSlot == -1) {
                            val targetView = target.openInventory
                            if (targetView.cursor == originalItem) {
                                targetView.setCursor(new)
                            } else {
                                rollbackTransaction(new)
                            }
                        } else {
                            if (inv.getItem(targetSlot) == originalItem) {
                                inv.setItem(targetSlot, new)
                            } else {
                                rollbackTransaction(new)
                            }
                        }
                    }
                    modifiedSlots.clear()

                    // Update inventory
                    fun setSlots(pairs: List<SlotPair>, inv: Inventory) {
                        for ((p, display) in pairs) {
                            inventory.setItem(display, inv.getItem(p))
                        }
                    }
                    cursorSlot?.let { slot ->
                        inventory.setItem(slot, target.openInventory.cursor)
                    }
                    setSlots(slotPairs, targetInv)
                    setSlots(craftSlotPairs, craftInv)
                }
            }
        }

        override fun onClose() {
            task?.cancel()
            task = null
        }

        override fun handleDrag(e: InventoryDragEvent) {
            if (!allowEdit) return super.handleDrag(e)

            val toExecute = mutableListOf<() -> Unit>()
            lock.withLock {
                for (slot in e.rawSlots) {
                    if (slot >= inventory.size) continue

                    if (slot == cursorSlot) {
                        modifiedSlots.computeIfAbsent(slot) {
                            PendingChange(targetInv, -1, target.openInventory.cursor)
                        }
                    } else if (dropSlots.contains(slot)) {
                        toExecute.add {
                            viewer.player.onTick {
                                val item = inventory.getItem(slot) ?: return@onTick
                                target.syncTick {
                                    inventory.setItem(slot, null)
                                    target.dropItem(item)
                                }
                            }
                        }
                    } else {
                        val inv: Inventory
                        val targetSlot: Int
                        val craftFind = craftSlotPairs.find { it.display == slot }
                        if (craftFind != null) {
                            inv = craftInv
                            targetSlot = craftFind.player
                        } else {
                            slotPairs.find { it.display == slot }?.let {
                                inv = targetInv
                                targetSlot = it.player
                            } ?: return e.setCancelled(true) // Modified unchangeable element
                        }
                        toExecute.add {
                            modifiedSlots.computeIfAbsent(slot) {
                                PendingChange(inv, targetSlot, inv.getItem(targetSlot))
                            }
                        }
                    }
                }
            }
            for (function in toExecute) {
                function.invoke()
            }
        }
        
        private data class SlotPair(val player: Int, val display: Int)
        private data class PendingChange(val inventory: Inventory, val targetSlot: Int, val originalItem: ItemStack?)

        companion object {
            val INVENTORY_VIEW_GETTER by Versioned(PlayerInventoryViewGetter::class.java)
        }

    }

}