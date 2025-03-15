package io.github.rothes.esu.bukkit.inventory

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.config.data.ItemData
import io.github.rothes.esu.bukkit.inventory.action.InventoryAction
import io.github.rothes.esu.bukkit.inventory.action.SimpleAction
import io.github.rothes.esu.core.configuration.ConfigurationPart
import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

abstract class PagedHolder<T: PagedHolder.PagedExtra, E>(
    inventoryData: InventoryData<T>,
    val page: Int,
    entries: List<E>,
    entryAction: String = "entry"
) : DynamicHolder<T>(inventoryData) {

    init {
        val listSlots = Int2ReferenceLinkedOpenHashMap<InventoryData.InventoryItem>()
        actionRegistry.register(
            InventoryAction.create(entryAction) { slot, item ->
                listSlots[slot] = item
                null
            }, SimpleAction.create("PrevPage") { slot, item ->
                defer {
                    if (page > 0) {
                        click(slot) { e -> prevPage().open(e.whoClicked) }
                        setItem(slot, item.item.item)
                    } else {
                        setItem(slot, inventoryData.extra.noPreviousPage.itemUnsafe)
                    }
                }
            }, SimpleAction.create("NextPage") { slot, item ->
                defer {
                    if (listSlots.isNotEmpty() && listSlots.size * (page + 1) < entries.size) {
                        click(slot) { e -> nextPage().open(e.whoClicked) }
                        setItem(slot, item.item.item)
                    } else {
                        setItem(slot, inventoryData.extra.noNextPage.itemUnsafe)
                    }
                }
            },
        )
        defer(10) {
            var i = listSlots.size * page
            for ((slot, item) in listSlots.sequencedEntrySet()) {
                if (i < entries.size) {
                    val entry = entries[i++]
                    setItem(slot, setEntryItem(item, entry))
                } else {
                    setItem(slot, inventoryData.extra.noEntry.itemUnsafe)
                }
            }
        }
    }

    abstract fun nextPage(): PagedHolder<T, E>
    abstract fun prevPage(): PagedHolder<T, E>
    abstract fun setEntryItem(item: InventoryData.InventoryItem, entry: E): ItemStack?

    open class PagedExtra (
        val noPreviousPage: ItemData = ItemData(Material.GRAY_STAINED_GLASS_PANE, displayName = ""),
        val noNextPage: ItemData = ItemData(Material.GRAY_STAINED_GLASS_PANE, displayName = ""),
        val noEntry: ItemData = ItemData(Material.AIR),
    ): ConfigurationPart

}