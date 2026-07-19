package io.github.rothes.esu.bukkit.util.version.adapter.nms.network

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.lib.adventure.inventory.Book
import org.bukkit.entity.Player

interface OpenBookPacketSender {

    fun openBook(player: Player, book: Book)

    companion object {

        val INSTANCE = versioned<OpenBookPacketSender>()

    }

}