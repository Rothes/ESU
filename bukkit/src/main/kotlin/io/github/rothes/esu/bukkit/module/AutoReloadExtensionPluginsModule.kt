package io.github.rothes.esu.bukkit.module

import bukkit.com.rylinaux.plugman.PlugManBukkit
import com.rylinaux.plugman.PlugMan
import io.github.rothes.esu.bukkit.inventory.EsuInvHolder
import io.github.rothes.esu.bukkit.module.AutoReloadExtensionPluginsModule.ModuleConfig
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.version.adapter.InventoryAdapter
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.util.extension.ConfigurationOptionsExt.headerIfNotNull
import io.github.rothes.esu.lib.org.spongepowered.configurate.yaml.YamlConfigurationLoader
import io.github.rothes.esu.lib.org.spongepowered.configurate.yaml.internal.snakeyaml.emitter.Emitter
import org.bukkit.Bukkit
import org.jetbrains.exposed.v1.core.statements.UpdateStatement

object AutoReloadExtensionPluginsModule: BukkitModule<ModuleConfig, EmptyConfiguration>(
    ModuleConfig::class.java, EmptyConfiguration::class.java
) {

    private lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override val configLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder
        get() = {
            it.defaultOptions { options ->
                options.headerIfNotNull("""
                    |This module will automatically reload the plugins that depend
                    |on ESU when ESU is reloaded with a plugin management plugin.
                    |Also, further improve the compatibility of hot reload/update ESU.
                    |PlugMan/PlugManX is required to enable this.
                    """.trimIndent().trimMargin()
                )
            }
        }

    override fun canUse(): Boolean {
        return super.canUse() && !plugin.initialized && Bukkit.getPluginManager().getPlugin("PlugMan") != null
    }

    override fun enable() {
        data = ConfigLoader.load(dataPath)
        loadCriticalClasses()
        if (!plugin.enabledHot)
            return

        for (pl in data.pluginsToLoad) {
            try {
                try {
                    // PlugMan v2
                    PlugMan.getInstance().pluginUtil.load(pl)
                } catch (_: NoClassDefFoundError) {
                    // PlugManX v3
                    PlugManBukkit.getInstance().pluginManager.load(pl)
                }
            } catch (e: Throwable) {
                plugin.err("Failed to load plugin $pl :", e)
            }
        }

        data.pluginsToLoad.clear()
    }

    @Suppress("DEPRECATION")
    override fun disable() {
        super.disable()
        if (plugin.isEnabled)
            return
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
            try {
                // PlugMan v2
                PlugMan.getInstance().pluginUtil.unload(it)
            } catch (_: NoClassDefFoundError) {
                // PlugManX v3
                val manager = PlugManBukkit.getInstance().pluginManager
                manager.unload(manager.getPluginByName(it.name))
            }
            data.pluginsToLoad.add(it.name)
        }
        data.pluginsToLoad.reverse()
        ConfigLoader.save(dataPath, data)
    }

    private fun loadCriticalClasses() {
        // Load the classes those are easily to break the hot plugin update.
        Emitter::class.java.declaredClasses // This may cause break when empty data loaded and saving with flow node
        org.incendo.cloud.parser.flag.FlagContext::class.java.toString()
        Charsets::class.java.toString()
        InventoryAdapter.instance
        EsuInvHolder::class.java.toString()
        UpdateStatement::class.java.toString()
    }

    data class ModuleData(
        val pluginsToLoad: MutableList<String> = arrayListOf(),
    )

    data class ModuleConfig(
        val pluginBlacklist: List<String> = listOf("EsuAddonPlugin"),
    ): BaseModuleConfiguration()
    
}