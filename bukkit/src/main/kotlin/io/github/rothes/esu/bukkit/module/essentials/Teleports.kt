package io.github.rothes.esu.bukkit.module.essentials

import io.github.rothes.esu.bukkit.module.essentials.teleports.Tpa
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonFeature

object Teleports : CommonFeature<Unit, Teleports.Lang>() {

    init {
        registerFeature(Tpa)
    }

    override fun onEnable() {}

    class Lang : ConfigurationPart

}
