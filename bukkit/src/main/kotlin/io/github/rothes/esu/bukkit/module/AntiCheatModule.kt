package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.anticheat.ExploitCheats
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration

object AntiCheatModule : BukkitModule<BaseModuleConfiguration, Unit>() {

    init {
        registerFeature(ExploitCheats)
    }

    override fun onEnable() {}

}