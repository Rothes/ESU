package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.vanillatweaks.SignBlock
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration

object VanillaTweaks : BukkitModule<VanillaTweaks.ModuleConfig, EmptyConfiguration>() {

    init {
        registerFeature(SignBlock)
    }

    override fun onEnable() {

    }

    class ModuleConfig: BaseModuleConfiguration()

}