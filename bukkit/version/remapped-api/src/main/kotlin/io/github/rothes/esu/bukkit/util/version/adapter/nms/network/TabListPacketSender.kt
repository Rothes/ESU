package io.github.rothes.esu.bukkit.util.version.adapter.nms.network

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.entity.Player

interface TabListPacketSender {

    fun sendPlayerListHeaderAndFooter(player: Player, header: Component, footer: Component)

    companion object {

        val INSTANCE = versioned<TabListPacketSender>()

    }

}