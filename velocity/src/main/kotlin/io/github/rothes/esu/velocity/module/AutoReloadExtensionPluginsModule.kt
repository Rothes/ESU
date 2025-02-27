package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.velocity.module.AutoReloadExtensionPluginsModule.ModuleConfig
import io.github.rothes.esu.velocity.plugin
import net.frankheijden.serverutils.velocity.managers.VelocityPluginManager
import org.incendo.cloud.parser.flag.FlagContext
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import org.spongepowered.configurate.yaml.internal.snakeyaml.emitter.Emitter
import org.spongepowered.configurate.yaml.internal.snakeyaml.parser.ParserImpl
import kotlin.jvm.optionals.getOrNull

object AutoReloadExtensionPluginsModule: VelocityModule<ModuleConfig, EmptyConfiguration>(
    ModuleConfig::class.java, EmptyConfiguration::class.java
) {

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
        loadCriticalClasses()
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
            !config.pluginBlacklist.contains(description.id) && description.dependencies.find { it.id == esuId } != null
        }.sortedWith { a, b ->
            val da = a.description
            val db = b.description
            val otherId = db.id
            if (da.dependencies.find { it.id == otherId } != null) -1 else 1
        }
        plugins.forEach {
            VelocityPluginManager.get().unloadPlugin(it)
            data.pluginsToLoad.add(it.description.id)
        }
        data.pluginsToLoad.reverse()
        ConfigLoader.save(dataPath, data)
    }

    private fun loadCriticalClasses() {
        // Load the classes those are easily to break the hot plugin update.
        Emitter::class.java.declaredClasses // This may cause break when empty data loaded and saving with flow node
        ParserImpl::class.java
        FlagContext::class.java
    }

    data class ModuleData(
        val pluginsToLoad: MutableList<String> = arrayListOf(),
    )

    data class ModuleConfig(
        val pluginBlacklist: List<String> = listOf("EsuAddonPlugin"),
    ): BaseModuleConfiguration()
    
}