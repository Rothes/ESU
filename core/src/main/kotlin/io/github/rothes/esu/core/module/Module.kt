package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiConfiguration
import java.nio.file.Path

interface Module<T: ConfigurationPart, L: ConfigurationPart> {

    val name: String
    val config: T
    val locale: MultiConfiguration<L>
    val moduleFolder: Path
    val configPath: Path
    val localePath: Path
    var enabled: Boolean

    fun canUse(): Boolean
    fun enable()
    fun disable()
    fun reloadConfig()

}