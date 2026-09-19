package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.anticheat.NoClientTeleportConfirm
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration

object AntiCheatModule : BukkitModule<BaseModuleConfiguration, Unit>() {

    init {
        registerFeature(NoClientTeleportConfirm)
    }

    override fun onEnable() {}

}