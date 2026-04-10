package io.github.rothes.esu.bukkit.util.version.adapter.adventure

import io.github.rothes.esu.lib.adventure.platform.facet.Facet
import net.minecraft.world.item.ItemStack
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
interface EsuNativeBook<T>: Facet.Book<Player, T, ItemStack>