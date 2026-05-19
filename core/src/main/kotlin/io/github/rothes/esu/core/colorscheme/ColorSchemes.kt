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

package io.github.rothes.esu.core.colorscheme

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.ConfigLoader

object ColorSchemes {

    var schemes: MultiColorSchemeConfiguration = load()
        private set

    fun reload() {
        schemes = load()
    }

    private fun load(): MultiColorSchemeConfiguration {
        return ConfigLoader.loadMulti<MultiColorSchemeConfiguration, ColorScheme>(
            EsuCore.instance.baseConfigPath().resolve("color_schemes"),
            ConfigLoader.LoaderSettingsMulti(
                EsuConfig.get().defaultColorScheme,
                yamlLoader = {
                    it.defaultOptions {
                        it.header("""
                        |The default color scheme for ESU.
                        |To use the color defined in color scheme, use <primary_color>, <primary_dim_color> or so.
                        |Alternatively, you can abridge them like <pc>, <pdc>.
                        |
                        |We make dim color of its original of HSL(+0°, -10%, -8%), error color excluded.
                    """.trimMargin())
                    }
                }
            ))
    }

}