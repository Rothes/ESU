package io.github.rothes.esu.bukkit.module.optimizations

import io.github.rothes.esu.bukkit.module.optimizations.antilag.WaterloggedFeature
import io.github.rothes.esu.core.module.CommonFeature

object AntiLagFeatures : CommonFeature<Unit, Unit>() {

    override val name: String = "AntiLag"

    init {
        registerFeature(WaterloggedFeature)
    }

    override fun onEnable() {}

}