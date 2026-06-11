package io.github.rothes.esu.core.util.lang

import io.github.rothes.esu.core.config.EsuConfig

object LangUtils {

    fun <V, R> Map<String, V>.getLangOrNull(lang: String? = EsuConfig.get().locale, block: (V) -> R?): R? {
        return this[lang]?.let(block)
            // If this lang is not found, try the same language.
            ?: lang?.substringBefore('_')?.let { language ->
                val sameLang = language + '_'
                this.entries.find { it.key.startsWith(sameLang) }?.let { block(it.value) }
            }
            // Still? Use the server default lang instead.
            ?: this[EsuConfig.get().locale]?.let(block)
            // Use the default value.
            ?: this["en_us"]?.let(block)
            // Maybe it doesn't provide en_us lang...?
            ?: this.values.firstNotNullOfOrNull { block(it) }
    }

}