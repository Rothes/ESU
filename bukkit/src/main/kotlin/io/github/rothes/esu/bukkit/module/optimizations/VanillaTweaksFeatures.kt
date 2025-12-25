package io.github.rothes.esu.bukkit.module.optimizations

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.BiomeSpawnersFeature
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.TicketTypeFeatures
import io.github.rothes.esu.core.module.CommonFeature

object VanillaTweaksFeatures : CommonFeature<Unit, Unit>() {

    override val name: String = "VanillaTweaks"

    init {
        registerFeature(BiomeSpawnersFeature)
        registerFeature(TicketTypeFeatures)
    }

    override fun onEnable() {}

}