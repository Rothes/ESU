package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.coroutine.IOScope
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import io.github.rothes.esu.velocity.module.AutoReloadExtensionPluginsModule.ModuleConfig
import io.github.rothes.esu.velocity.plugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.frankheijden.serverutils.velocity.managers.VelocityPluginManager
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.seconds

object AutoReloadExtensionPluginsModule: VelocityModule<ModuleConfig, EmptyConfiguration>() {

    private lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (plugin.initialized)
                return Feature.AvailableCheck.fail { "Esu is already initialized".message }
            if (plugin.server.pluginManager.getPlugin("serverutils") == null)
                return Feature.AvailableCheck.fail { "ServerUtils not found".message }
            null
        }
    }

    override fun onEnable() {
        data = ConfigLoader.load(dataPath)

        val toLoad = data.pluginsToLoad.toList()
        data.pluginsToLoad.clear()

        if (!plugin.enabledHot)
            return

        for (pl in toLoad) {
            val serverUtils = VelocityPluginManager.get()
            serverUtils.getPluginFile(pl).getOrNull()?.let {
                val load = serverUtils.loadPlugin(it)
                if (!load.isSuccess) {
                    plugin.warn("[AutoReloadExtensionPlugins] Failed to load plugin $pl: ${load.result.name.lowercase()}")
                    continue
                }
                runBlocking {
                    // serverUtils.enablePlugin infinite loop, so we don't know if it's completed
                    // https://github.com/FrankHeijden/ServerUtils/pull/72#issuecomment-2577691669
                    IOScope.launch {
                        val enable = serverUtils.enablePlugin(load.plugin)
                        if (!enable.isSuccess) {
                            plugin.warn("[AutoReloadExtensionPlugins] Failed to enable plugin $pl: ${load.result.name.lowercase()}")
                        }
                    }
                    delay(0.5.seconds)
                    plugin.info("[AutoReloadExtensionPlugins] Load plugin $pl")
                }
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        if (plugin.enabled || !plugin.disabledHot)
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
        for (pl in plugins) {
            val serverUtils = VelocityPluginManager.get()
            val disable = serverUtils.disablePlugin(pl)
            if (!disable.isSuccess) {
                plugin.warn("[AutoReloadExtensionPlugins] Failed to disable plugin ${pl.description.id}: ${disable.result.name.lowercase()}")
                continue
            }
            val unload = serverUtils.unloadPlugin(disable.plugin)
            if (!unload.isSuccess) {
                plugin.warn("[AutoReloadExtensionPlugins] Failed to unload plugin ${pl.description.id}: ${disable.result.name.lowercase()}")
                continue
            }

            data.pluginsToLoad.add(pl.description.id)
            plugin.info("[AutoReloadExtensionPlugins] Unloaded plugin ${pl.description.id}")
        }
        data.pluginsToLoad.reverse()
        ConfigLoader.save(dataPath, data)
        dataPath.toFile().deleteOnExit()
    }

    override fun buildConfigLoader(builder: YamlConfigurationLoader.Builder) {
        super.buildConfigLoader(builder)
        builder.defaultOptions {
            it.header("""
                    This module will automatically reload the plugins that depend
                    on ESU when ESU is reloaded with a plugin management plugin.
                    ServerUtils is required to enable this.
                    """.trimIndent()
            )
        }
    }

    data class ModuleData(
        val pluginsToLoad: MutableList<String> = arrayListOf(),
    )

    data class ModuleConfig(
        val pluginBlacklist: List<String> = listOf("EsuAddonPlugin"),
    ): BaseModuleConfiguration()
    
}