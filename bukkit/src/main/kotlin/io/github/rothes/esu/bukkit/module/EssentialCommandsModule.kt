package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.essencialcommands.*
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration

object EssentialCommandsModule: BukkitModule<BaseModuleConfiguration, EssentialCommandsModule.ModuleLang>() {

    init {
        listOf(
            ClientLocale, Heal, Ip, IpGroup, Ping, PlayerChunkTickets, TpChunk
        ).forEach { cmd -> registerFeature(cmd) }
    }

    override fun onEnable() {}

    data class ModuleLang(
        val unsafeTeleportSpot: MessageData = "<ec>Cannot find a safe spot for teleport.".message,
    ): ConfigurationPart

}