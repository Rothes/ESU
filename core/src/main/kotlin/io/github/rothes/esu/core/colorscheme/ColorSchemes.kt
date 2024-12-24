package io.github.rothes.esu.core.colorscheme

import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader

object ColorSchemes {

    var schemes: MultiColorSchemeConfiguration = load()
        private set

    fun reload() {
        schemes = load()
    }

    private fun load(): MultiColorSchemeConfiguration {
        return ConfigLoader.loadMulti(
            EsuCore.instance.baseConfigPath().resolve("color_schemes"),
            "${EsuConfig.get().defaultColorScheme}.yml",
            builder = {
                it.defaultOptions {
                    it.header("""
                        |The default color scheme for ESU.
                        |To use the color defined in color scheme, use <primary_color>, <primary_dim_color> or so.
                        |Alternatively, you can abridge them like <pc>, <pdc>.
                        |
                        |We make dim color of its original of HSL(+0°, -10%, -8%), error color excluded.
                    """.trimMargin())
                }
            })
    }

}