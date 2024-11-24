package io.github.rothes.esu.core.colorscheme

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader

object ColorSchemes {

    var schemes: MultiColorSchemeConfiguration = load()
        private set

    fun reload() {
        schemes = load()
    }

    private fun load(): MultiColorSchemeConfiguration {
        return ConfigLoader.loadMulti(EsuCore.instance.baseConfigPath().resolve("color_schemes"), "amethyst.yml")
    }

}