package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.core.util.InitOnce
import java.nio.file.Path

abstract class SavableConfiguration: ConfigurationPart {

    var path: Path by InitOnce()

    fun save() {
        ConfigLoader.save(path, this)
    }

}