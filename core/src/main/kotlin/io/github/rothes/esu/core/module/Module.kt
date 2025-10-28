package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.configuration.ConfigurationPart
import java.nio.file.Path

interface Module<C: ConfigurationPart, L: ConfigurationPart>: Feature<C, L> {

    val moduleFolder: Path
    val configPath: Path
    val langPath: Path

    fun doReload() {
        onReload()
        for (child in getFeatures()) {
            child.onReload()
        }
    }

}