package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.velocity.module.serverinfo.MotdFeature
import io.github.rothes.esu.velocity.module.serverinfo.RebrandFeature

object ServerInfoModule : VelocityModule<BaseModuleConfiguration, Unit>() {

    init {
        registerFeature(RebrandFeature)
        registerFeature(MotdFeature)
    }

    override fun onEnable() {}

}