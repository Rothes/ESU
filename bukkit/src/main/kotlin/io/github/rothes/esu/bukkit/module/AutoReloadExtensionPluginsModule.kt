package io.github.rothes.esu.bukkit.module

import bukkit.com.rylinaux.plugman.PlugManBukkit
import com.rylinaux.plugman.PlugMan
import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.inventory.EsuInvHolder
import io.github.rothes.esu.bukkit.module.AutoReloadExtensionPluginsModule.ModuleConfig
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.version.adapter.InventoryAdapter
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.extension.headerIfNotNull
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import org.bukkit.Bukkit
import org.incendo.cloud.annotations.Command

object AutoReloadExtensionPluginsModule: BukkitModule<ModuleConfig, EmptyConfiguration>() {

    private lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (core.initialized)
                return Feature.AvailableCheck.fail { "Esu is already initialized".message }
            if (!listOf("PlugMan", "PlugManX").any { Bukkit.getPluginManager().isPluginEnabled(it) })
                return Feature.AvailableCheck.fail { "PlugMan not found".message }
            null
        }
    }

    override fun onEnable() {
        data = ConfigLoader.load(dataPath)
        loadCriticalClasses()
        registerCommands(object {
            @Command("esu autoReloadExtensionPlugins updateCommands")
            @ShortPerm
            fun updateCommands(user: User) {
                Bukkit.getOnlinePlayers().forEach {
                    it.updateCommands()
                }
                user.message("OK!")
            }
        })

        val toLoad = data.pluginsToLoad.toList()
        data.pluginsToLoad.clear()

        if (!core.enabledHot)
            return

        for (pl in toLoad) {
            if (Bukkit.getPluginManager().isPluginEnabled(pl)) {
                core.warn("[AutoReloadExtensionPlugins] Plugin $pl is already enabled")
                continue
            }
            try {
                try {
                    // PlugMan v2
                    PlugMan.getInstance().pluginUtil.load(pl)
                } catch (_: NoClassDefFoundError) {
                    // PlugManX v3
                    PlugManBukkit.getInstance().pluginManager.load(pl)
                }
            } catch (e: Throwable) {
                core.err("Failed to load plugin $pl :", e)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onDisable() {
        super.onDisable()
        if (core.isEnabled || !core.disabledHot)
            return
        val esuName = plugin.description.name
        val plugins = Bukkit.getPluginManager().plugins.filter {
            val description = it.description
            it.isEnabled && !config.pluginBlacklist.contains(description.name)
                    && (description.depend.contains(esuName) || description.softDepend.contains(esuName))
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
        dataPath.toFile().deleteOnExit()
    }

    override fun buildConfigLoader(builder: YamlConfigurationLoader.Builder) {
        super.buildConfigLoader(builder)
        builder.defaultOptions { options ->
            options.headerIfNotNull("""
                    This module will automatically reload the plugins that depend
                    on ESU when ESU is reloaded with a plugin management plugin.
                    Also, further improve the compatibility of hot reload/update ESU.
                    PlugMan/PlugManX is required to enable this.
                    """.trimIndent()
            )
        }
    }

    private fun loadCriticalClasses() {
        // Load the classes those are easily to break the hot plugin update.
        InventoryAdapter.instance
        EsuInvHolder::class.java.toString()
    }

    data class ModuleData(
        val pluginsToLoad: MutableList<String> = arrayListOf(),
    )

    data class ModuleConfig(
        val pluginBlacklist: List<String> = listOf("EsuAddonPlugin"),
    ): BaseModuleConfiguration()
    
}