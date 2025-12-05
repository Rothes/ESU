package io.github.rothes.esu.core.util.extension

import io.github.rothes.esu.lib.configurate.ConfigurationOptions

fun ConfigurationOptions.headerIfNotNull(header: String): ConfigurationOptions {
    return if (header() == null) header(header) else this
}