package io.github.rothes.esu.core.util.extension

import io.github.rothes.esu.lib.org.spongepowered.configurate.ConfigurationOptions

object ConfigurationOptionsExt {

    fun ConfigurationOptions.headerIfNotNull(header: String): ConfigurationOptions {
        return if (header() == null) header(header) else this
    }

}