package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import io.github.rothes.esu.velocity.module.AutoReloadExtensionPluginsModule.ModuleConfig
import io.github.rothes.esu.velocity.plugin
import net.frankheijden.serverutils.velocity.managers.VelocityPluginManager
import kotlin.jvm.optionals.getOrNull

object AutoReloadExtensionPluginsModule: VelocityModule<ModuleConfig, EmptyConfiguration>() {

    private lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override val configLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder
        get() = {
            it.defaultOptions {
                it.header("""
                    |This module will automatically reload the plugins that depend
                    |on ESU when ESU is reloaded with a plugin management plugin.
                    |ServerUtils is required to enable this.
                    """.trimIndent().trimMargin()
                )
            }
        }

    override fun canUse(): Boolean {
        return super.canUse() && !plugin.initialized && plugin.server.pluginManager.getPlugin("serverutils") != null
    }

    override fun enable() {
        data = ConfigLoader.load(dataPath)
        if (!plugin.enabledHot)
            return

        for (plugin in data.pluginsToLoad) {
            val serverUtils = VelocityPluginManager.get()
            serverUtils.getPluginFile(plugin).getOrNull()?.let {
                serverUtils.loadPlugin(it)
            }
        }

        data.pluginsToLoad.clear()
    }

    @Suppress("DEPRECATION")
    override fun disable() {
        super.disable()
        if (plugin.enabled)
            return
        val esuId = plugin.container.description.id
        val plugins = plugin.server.pluginManager.plugins.filter {
            val description = it.description
            !config.pluginBlacklist.contains(description.id) && description.dependencies.any { p -> p.id == esuId }
        }.sortedWith { a, b ->
            val da = a.description
            val db = b.description
            val otherId = db.id
            if (da.dependencies.any { it.id == otherId }) -1 else 1
        }
        plugins.forEach {
            VelocityPluginManager.get().unloadPlugin(it)
            data.pluginsToLoad.add(it.description.id)
        }
        data.pluginsToLoad.reverse()
        ConfigLoader.save(dataPath, data)
    }

    data class ModuleData(
        val pluginsToLoad: MutableList<String> = arrayListOf(),
    )

    data class ModuleConfig(
        val pluginBlacklist: List<String> = listOf("EsuAddonPlugin"),
    ): BaseModuleConfiguration()
    
}