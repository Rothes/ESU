package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class CommonModule<T: ConfigurationPart, L: ConfigurationPart>(
    val dataClass: Class<T>,
    val localeClass: Class<L>,
): Module<T, L> {

    constructor(dataClass: KClass<T>, localeClass: KClass<L>): this(dataClass.java, localeClass.java)

    override val name: String = javaClass.simpleName.removeSuffix("Module")
    override lateinit var config: T
        protected set
    override lateinit var locale: MultiLocaleConfiguration<L>
        protected set

    override val moduleFolder: Path by lazy {
        EsuCore.instance.baseConfigPath().resolve("modules").resolve(name)
    }
    override val configPath: Path by lazy {
        moduleFolder.resolve("config.yml")
    }
    override val localePath: Path by lazy {
        moduleFolder.resolve("locale")
    }
    override var enabled: Boolean = false

    override fun canUse(): Boolean {
        return (config as? BaseModuleConfiguration)?.moduleEnabled ?: true
    }

    override fun reloadConfig() {
        config = ConfigLoader.load(configPath, dataClass)
        locale = ConfigLoader.loadMulti(localePath, localeClass, "en_us.yml")
    }

    fun User.hasPerm(shortPerm: String): Boolean {
        return hasPermission(perm(shortPerm))
    }

    open fun perm(shortPerm: String): String = "esu.${this@CommonModule.name.lowercase()}.$shortPerm"

}