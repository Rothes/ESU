package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.EsuNativeBook
import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGetT
import io.github.rothes.esu.lib.adventure.platform.facet.Facet
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
object EsuAdventure {

    fun initAudience() {
        val adventureAudience = Class.forName("io.github.rothes.esu.lib.adventure.platform.bukkit.BukkitAudience")
        val facetBook = adventureAudience.getDeclaredField("BOOK")
            .accessibleGetT<MutableList<Facet.Book<Player, *, *>>>(null)
        val esuNativeBook by Versioned(EsuNativeBook::class.java)
        if (esuNativeBook.isSupported) {
            facetBook.add(0, esuNativeBook)
        }
    }

}