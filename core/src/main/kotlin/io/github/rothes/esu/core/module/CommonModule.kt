package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import org.incendo.cloud.Command
import java.lang.reflect.ParameterizedType
import java.nio.file.Path

abstract class CommonModule<C: ConfigurationPart, L: ConfigurationPart> : Module<C, L> {

    val configClass: Class<C>
    val localeClass: Class<L>

    init {
        val actualTypeArguments = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments

        @Suppress("UNCHECKED_CAST")
        configClass = actualTypeArguments[0] as Class<C>
        @Suppress("UNCHECKED_CAST")
        localeClass = actualTypeArguments[1] as Class<L>
    }

    override val name: String = javaClass.simpleName.removeSuffix("Module")
    override lateinit var config: C
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
        config = ConfigLoader.load(configPath, configClass, ConfigLoader.LoaderSettings(yamlLoader = configLoader))
        locale = ConfigLoader.loadMulti(localePath, localeClass, ConfigLoader.LoaderSettingsMulti("en_us", yamlLoader = localeLoader))
    }

    fun User.hasPerm(shortPerm: String): Boolean {
        return hasPermission(perm(shortPerm))
    }

    open fun perm(shortPerm: String): String = "esu.${name.lowercase()}.${shortPerm.lowercase()}"

}