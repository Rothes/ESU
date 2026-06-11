/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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