package io.github.rothes.esu.bukkit.util.version.adapter.adventure

import io.github.rothes.esu.lib.adventure.platform.facet.Facet
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@Suppress("UnstableApiUsage")
interface EsuNativeBook<T>: Facet.Book<Player, T, ItemStack>