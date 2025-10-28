package io.github.rothes.esu.core.module.configuration

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.MoveToTop

open class BaseModuleConfiguration(
    @MoveToTop
    private val moduleEnabled: Boolean = false,
): EnableTogglable, ConfigurationPart {

    override val enabled: Boolean
        get() = moduleEnabled

}