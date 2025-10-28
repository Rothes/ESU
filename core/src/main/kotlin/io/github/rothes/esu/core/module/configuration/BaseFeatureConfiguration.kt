package io.github.rothes.esu.core.module.configuration

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.MoveToTop

open class BaseFeatureConfiguration(
    @MoveToTop
    val enabled: Boolean = false,
): ConfigurationPart