package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.org.spongepowered.configurate.yaml.YamlConfigurationLoader
import org.incendo.cloud.Command
import java.nio.file.Path
import kotlin.jvm.java
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
    protected open val configLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it }
    protected open val localeLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it }

    protected val registeredCommands = LinkedHashSet<Command<out User>>()

    protected fun unregisterCommands() {
        with(EsuCore.instance.commandManager) {
            registeredCommands.forEach {
                deleteRootCommand(it.rootComponent().name())
            }
            registeredCommands.clear()
        }
    }

    override fun disable() {
        unregisterCommands()
    }

    override val moduleFolder: Path by lazy {
        EsuCore.instance.baseConfigPath().resolve("modules").resolve(name)
    }
    override val configPath: Path by lazy {
        moduleFolder.resolve("config.yml")
    }
    override val localePath: Path by lazy {
        moduleFolder.resolve("lang")
    }
    override var enabled: Boolean = false

    override fun canUse(): Boolean {
        return (config as? BaseModuleConfiguration)?.moduleEnabled ?: true
    }

    override fun reloadConfig() {
        config = ConfigLoader.load(configPath, dataClass, builder = configLoader)
        locale = ConfigLoader.loadMulti(localePath, localeClass, "en_us.yml", builder = localeLoader)
    }

    fun User.hasPerm(shortPerm: String): Boolean {
        return hasPermission(perm(shortPerm))
    }

    open fun perm(shortPerm: String): String = "esu.${name.lowercase()}.${shortPerm.lowercase()}"

}