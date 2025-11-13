package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.essencialcommands.Heal
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration

object EssentialCommandsModule: BukkitModule<BaseModuleConfiguration, EssentialCommandsModule.ModuleLang>() {

    init {
        registerFeature(Heal)
    }

    override fun onEnable() {}

    class ModuleLang: ConfigurationPart

}