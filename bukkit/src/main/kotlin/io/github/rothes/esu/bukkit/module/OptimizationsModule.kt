package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.optimizations.AntiLagFeatures
import io.github.rothes.esu.bukkit.module.optimizations.VanillaTweaksFeatures
import io.github.rothes.esu.core.configuration.LoadedConfiguration
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration

object OptimizationsModule: BukkitModule<OptimizationsModule.ModuleConfig, EmptyConfiguration>() {

    init {
        registerFeature(AntiLagFeatures)
        registerFeature(VanillaTweaksFeatures)
    }

    override fun onEnable() { }

    override fun preprocessConfig(loadedConfiguration: LoadedConfiguration) {
        // v0.14.0
        val node = loadedConfiguration.node
        val list = listOf(
            "ticket-type" to "vanilla-tweaks",
            "waterlogged" to "anti-lag",
        )
        for ((key, value) in node.childrenMap()) {
            val pair = list.find { it.first == key } ?: continue
            node.node(pair.second, pair.first).from(value)
            node.removeChild(key)
        }
    }

    class ModuleConfig : BaseModuleConfiguration()

}