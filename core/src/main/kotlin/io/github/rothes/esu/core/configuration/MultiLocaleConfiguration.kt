package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.EsuConfig

class MultiLocaleConfiguration<T : ConfigurationPart>(configs: Map<String, T>) : MultiConfiguration<T>(configs) {

    override fun <R> get(key: String?, block: (T) -> R?): R? {
        return configs[key]?.let(block)
            // If this locale is not found, try the same language.
            ?: key?.split('_')?.get(0)?.let { language ->
                val lang = language + '_'
                configs.entries.filter { it.key.startsWith(lang) }.firstNotNullOfOrNull { block(it.value) }
            // Still? Use the server default locale instead.
            ?: configs[EsuConfig.get().locale]?.let(block)
            // Use the default value.
            ?: configs["en_us"]?.let(block)
            // May it doesn't provide en_us locale...?
            } ?: configs.values.firstNotNullOfOrNull { block(it) }
    }

    override fun toString(): String {
        return "MultiLocaleConfiguration(configs=$configs)"
    }

}