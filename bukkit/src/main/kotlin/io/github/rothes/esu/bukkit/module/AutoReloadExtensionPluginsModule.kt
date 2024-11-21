package io.github.rothes.esu.bukkit.module

import com.rylinaux.plugman.PlugMan
import io.github.rothes.esu.bukkit.module.AutoReloadExtensionPluginsModule.ModuleConfig
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import org.bukkit.Bukkit
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import kotlin.jvm.java

object AutoReloadExtensionPluginsModule: BukkitModule<ModuleConfig, EmptyConfiguration>(
    ModuleConfig::class.java, EmptyConfiguration::class.java
) {

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

    @Suppress("DEPRECATION")
    override fun enable() {
        val esuName = plugin.description.name
        val plugins = Bukkit.getPluginManager().plugins.filter {
                val description = it.description
                it.isEnabled && description.depend.contains(esuName) || description.softDepend.contains(esuName)
            }.sortedWith { a, b ->
                val da = a.description
                val db = b.description
                val otherName = db.name
                if (da.depend.contains(otherName) || da.softDepend.contains(otherName)) -1 else 1
            }
        plugins.reversed().forEach {
            PlugMan.getInstance().pluginUtil.unload(it)
        }
        plugins.forEach {
            PlugMan.getInstance().pluginUtil.load(it.name)
        }
    }

    data class ModuleConfig(
        val pluginBlacklist: List<String> = listOf("EsuAddonPlugin"),
    ): BaseModuleConfiguration()
    
}