package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.core.CoreController
import io.github.rothes.esu.bukkit.module.core.DisabledProviders
import io.github.rothes.esu.bukkit.module.core.Providers
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.util.extension.headerIfNotNull
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader

object CoreModule: BukkitModule<CoreModule.ModuleConfig, Unit>() {

    private var _providers: Providers = DisabledProviders

    val providers: Providers get() = _providers

    override fun onEnable() {
        CoreController.onEnable()
        _providers = CoreController.RunningProviders
    }

    override fun onDisable() {
        super.onDisable()
        CoreController.onDisable()
        _providers = DisabledProviders
    }

    override fun buildConfigLoader(builder: YamlConfigurationLoader.Builder) {
        super.buildConfigLoader(builder)
        builder.defaultOptions { options ->
            options.headerIfNotNull("""
                This module will provides some important information for other modules to use,
                which doesn't create performance impact to your server.
                This is enabled by default, do not change if you don't know what you are doing.
            """.trimIndent())
        }
    }

    class ModuleConfig: BaseModuleConfiguration(true)

}