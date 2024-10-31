package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.EsuConfig

class MultiLocaleConfiguration<T : ConfigurationPart>(configs: Map<String, T>) : MultiConfiguration<T>(configs) {

    override fun <R> get(key: String?, block: (T) -> R?): R? {
        return configs[key]?.let(block)
            ?: key?.split('_')?.get(0)?.let { language ->
                val lang = language + '_'
                configs.entries.filter { it.key.startsWith(lang) }.firstNotNullOfOrNull { block(it.value) }
            ?: configs[EsuConfig.get().locale]?.let(block)
            ?: configs["en_us"]?.let(block)
            } ?: configs.values.firstNotNullOfOrNull { block(it) }
    }

    override fun toString(): String {
        return "MultiLocaleConfiguration(configs=$configs)"
    }

}