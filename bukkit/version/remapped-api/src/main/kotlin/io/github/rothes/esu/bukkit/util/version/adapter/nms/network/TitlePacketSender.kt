package io.github.rothes.esu.bukkit.util.version.adapter.nms.network

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.entity.Player

interface TitlePacketSender {

    fun sendActionBar(player: Player, message: Component)

    fun sendTitlesAnimation(player: Player, fadeIn: Int, stay: Int, fadeOut: Int)
    fun sendTitle(player: Player, message: Component)
    fun sendSubtitle(player: Player, message: Component)

    fun clearTitle(player: Player)
    fun resetTitle(player: Player)

    companion object {

        val INSTANCE = versioned<TitlePacketSender>()

    }

}