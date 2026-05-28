package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.essentials.Teleports
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration

object EssentialsModule : BukkitModule<EssentialsModule.ModuleConfig, EmptyConfiguration>() {

    init {
        registerFeature(Teleports)
    }

    override fun onEnable() {}

    class ModuleConfig : BaseModuleConfiguration()

}
