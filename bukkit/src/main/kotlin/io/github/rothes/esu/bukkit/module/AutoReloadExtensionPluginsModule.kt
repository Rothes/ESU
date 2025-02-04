package io.github.rothes.esu.bukkit.module

import com.rylinaux.plugman.PlugMan
import io.github.rothes.esu.bukkit.module.AutoReloadExtensionPluginsModule.ModuleConfig
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import org.bukkit.Bukkit
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import kotlin.jvm.java

object AutoReloadExtensionPluginsModule: BukkitModule<ModuleConfig, EmptyConfiguration>(
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
                    |PlugMan/PlugManX is required to enable this.
                    """.trimIndent().trimMargin()
                )
            }
        }

    override fun canUse(): Boolean {
        return super.canUse() && !plugin.initialized && Bukkit.getPluginManager().getPlugin("PlugMan") != null
    }

    override fun reloadConfig() {
        super.reloadConfig()
        data = ConfigLoader.load(dataPath)
    }

    @Suppress("DEPRECATION")
    override fun enable() {
        if (!plugin.enabledHot)
            return
        for (plugin in data.pluginsToLoad) {
            PlugMan.getInstance().pluginUtil.load(plugin)
        }
        data.pluginsToLoad.clear()
    }

    override fun disable() {
        val esuName = plugin.description.name
        val plugins = Bukkit.getPluginManager().plugins.filter {
            val description = it.description
            it.isEnabled && !config.pluginBlacklist.contains(description.name)
                    && description.depend.contains(esuName) || description.softDepend.contains(esuName)
        }.sortedWith { a, b ->
            val da = a.description
            val db = b.description
            val otherName = db.name
            if (da.depend.contains(otherName) || da.softDepend.contains(otherName)) -1 else 1
        }
        plugins.forEach {
            PlugMan.getInstance().pluginUtil.unload(it)
            data.pluginsToLoad.add(it.name)
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