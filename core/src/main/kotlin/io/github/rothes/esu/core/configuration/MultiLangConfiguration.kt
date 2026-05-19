/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.core.config.EsuConfig

class MultiLangConfiguration<T>(configs: Map<String, T>) : MultiConfiguration<T>(configs) {

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
            // Maybe it doesn't provide en_us locale...?
            } ?: configs.values.firstNotNullOfOrNull { block(it) }
    }

    override fun toString(): String {
        return "MultiLocaleConfiguration(configs=$configs)"
    }

}