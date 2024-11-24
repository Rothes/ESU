package io.github.rothes.esu.core.colorscheme

import io.github.rothes.esu.EsuConfig
import io.github.rothes.esu.core.configuration.MultiConfiguration

class MultiColorSchemeConfiguration(configs: Map<String, ColorScheme>) : MultiConfiguration<ColorScheme>(configs) {

    override fun <R> get(key: String?, block: ColorScheme.() -> R?): R? {
        return configs[key]?.let(block)
            ?: configs[EsuConfig.get().defaultColorScheme]?.let(block)
            ?: configs.values.firstNotNullOfOrNull { block(it) }
    }

    override fun toString(): String {
        return "MultiColorSchemeConfiguration(configs=$configs)"
    }

}