package io.github.rothes.esu.bukkit.inventory

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.config.data.ItemData
import io.github.rothes.esu.bukkit.inventory.type.SimpleType
import io.github.rothes.esu.core.configuration.ConfigurationPart
import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

abstract class PagedHolder<T: PagedHolder.PagedFallback, E>(
    inventoryData: InventoryData<T>,
    val page: Int,
    entries: List<E>,
    entryType: String = "Entry"
) : DynamicHolder<T>(inventoryData) {

    init {
        val listSlots = Int2ReferenceLinkedOpenHashMap<InventoryData.InventoryItem>()
        typeRegistry.register(
            SimpleType.create(entryType) { slot, item ->
                listSlots[slot] = item
                null
            }, SimpleType.create("PrevPage") { slot, item ->
                defer {
                    if (page > 0) {
                        click(slot) { e -> prevPage().open(e.whoClicked) }
                        setItem(slot, item.item.item)
                    } else {
                        setItem(slot, inventoryData.fallback.noPreviousPage.itemUnsafe)
                    }
                }
            }, SimpleType.create("NextPage") { slot, item ->
                defer {
                    if (listSlots.isNotEmpty() && listSlots.size * (page + 1) < entries.size) {
                        click(slot) { e -> nextPage().open(e.whoClicked) }
                        setItem(slot, item.item.item)
                    } else {
                        setItem(slot, inventoryData.fallback.noNextPage.itemUnsafe)
                    }
                }
            },
        )
        defer(10) {
            var i = listSlots.size * page
            for ((slot, item) in listSlots.sequencedEntrySet()) {
                if (i < entries.size) {
                    val entry = entries[i++]
                    setItem(slot, setEntryItem(slot, item, entry))
                } else {
                    setItem(slot, inventoryData.fallback.noEntry.itemUnsafe)
                }
            }
        }
    }

    abstract fun nextPage(): PagedHolder<T, E>
    abstract fun prevPage(): PagedHolder<T, E>
    abstract fun setEntryItem(slot: Int, item: InventoryData.InventoryItem, entry: E): ItemStack?

    open class PagedFallback (
        val noPreviousPage: ItemData = ItemData(Material.GRAY_STAINED_GLASS_PANE, displayName = ""),
        val noNextPage: ItemData = ItemData(Material.GRAY_STAINED_GLASS_PANE, displayName = ""),
        val noEntry: ItemData = ItemData(Material.AIR),
    ): ConfigurationPart

}