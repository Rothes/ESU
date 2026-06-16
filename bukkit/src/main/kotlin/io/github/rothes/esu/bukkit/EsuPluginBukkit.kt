/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.command.EsuBukkitCommandManager
import io.github.rothes.esu.bukkit.config.BukkitEsuLang
import io.github.rothes.esu.bukkit.listener.InternalListeners
import io.github.rothes.esu.bukkit.module.*
import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.util.BukkitDataSerializer
import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.inventory.InventoryUtils
import io.github.rothes.esu.bukkit.util.inventory.InventoryUtils.isEsuInventory
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.VersionedInstance
import io.github.rothes.esu.bukkit.util.version.adapter.InventoryAdapter.Companion.topInv
import io.github.rothes.esu.bukkit.util.version.remapper.JarRemapper
import io.github.rothes.esu.common.HotLoadSupport
import io.github.rothes.esu.common.command.EsuAdminCommand
import io.github.rothes.esu.common.module.AutoBroadcastModule
import io.github.rothes.esu.common.util.extension.shutdown
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.esu.core.util.extension.ClassUtils.jarFile
import io.github.rothes.esu.lib.bstats.bukkit.Metrics
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.incendo.cloud.CommandManager
import java.util.logging.Level

class EsuPluginBukkit(
    val bootstrap: EsuBootstrapBukkit
): EsuCoreBukkit {

    override var initialized: Boolean = false
        private set

    override val baseCommandNode: String = "esu"

    override var enabledHot: Boolean by InitOnce()
    override var disabledHot: Boolean by InitOnce()

    init {
        EsuCore.instance = this
        BukkitDataSerializer // Register bukkit serializers

        loadVersions()
        enabledHot = byPlugMan()
    }

    private fun loadVersions() {
        val tempFolder = bootstrap.dataFolder.resolve(".cache/minecraft_versions")
        tempFolder.deleteRecursively()
        tempFolder.mkdirs()

        javaClass.jarFile.use { jarFile ->
            val entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val fullName = entry.name
                if (fullName.startsWith("esu_minecraft_versions/") && fullName != "esu_minecraft_versions/") {
                    val name = fullName.substringAfterLast("/")
                    val file = tempFolder.resolve(name)
                    file.createNewFile()

                    jarFile.getInputStream(entry).use { stream ->
                        file.outputStream().use { output ->
                            stream.copyTo(output)
                        }
                    }

                    val toLoad = if (!ServerInfo.isMojmap && ServerInfo.hasMojmap) JarRemapper.reobf(file) else file
                    VersionedInstance.loadVersionJar(toLoad)
                }
            }
        }
    }

    override val commandManager: CommandManager<User> by lazy { EsuBukkitCommandManager() }

    fun onLoad() {
    }

    fun onEnable() {
        adventure           // Init adventure
        EsuConfig           // Load global config
        BukkitEsuLang       // Load global lang
        StorageManager      // Load database
        ColorSchemes        // Load color schemes
        UpdateCheckerMan    // Init update checker
        BukkitUserManager   // Init user manager
        ServerHotLoadSupport(enabledHot).onEnable()
        EsuAdventure.inject()

        Bukkit.getOnlinePlayers().forEach { it.user }

        ModuleManager.addModule(CoreModule)
        ModuleManager.addModule(AutoBroadcastModule)
        ModuleManager.addModule(AutoRestartModule)
        ModuleManager.addModule(BetterEventMessagesModule)
        ModuleManager.addModule(BlockedCommandsModule)
        ModuleManager.addModule(ChatAntiSpamModule)
        ModuleManager.addModule(CommandAntiSpamModule)
        ModuleManager.addModule(EssentialCommandsModule)
        ModuleManager.addModule(EsuChatModule)
        ModuleManager.addModule(ExploitFixesModule)
        ModuleManager.addModule(ItemEditModule)
        ModuleManager.addModule(NetworkThrottleModule)
        ModuleManager.addModule(SocialFilterModule)
        ModuleManager.addModule(SpawnProtectModule)
        ModuleManager.addModule(NewsModule)
        ModuleManager.addModule(OptimizationsModule)
        ModuleManager.addModule(SpoofServerSettingsModule)
        ModuleManager.addModule(VanillaTweaksModule)

        ModuleManager.addModule(AutoReloadExtensionPluginsModule)

        // Register commands
        EsuAdminCommand.register {
            UpdateCheckerMan.reload()
        }

        Bukkit.getOnlinePlayers().forEach { it.updateCommands() }
        InternalListeners // Init

        Metrics(bootstrap, 24645) // bStats
        Scheduler.global {
            initialized = true
        }
    }

    fun onDisable() {
        disabledHot = byPlugMan()
        ServerHotLoadSupport(disabledHot).onDisable()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.removeModule(it) }
        commandManager.shutdown()

        for (player in Bukkit.getOnlinePlayers()) {
            player.updateCommands() // We have removed all our commands, update it

            player.syncTick {
                try {
                    if (player.openInventory.topInv.isEsuInventory)
                        player.closeInventory()
                } catch (e: Exception) {
                    bootstrap.logger.log(Level.WARNING, "Failed to handle inventory", e)
                }
            }

            BukkitUserManager.getCache(player.uniqueId)?.let { user ->
                try {
                    StorageManager.updateUserNow(user)
                } catch (t: Throwable) {
                    err("Failed to update user ${user.nameUnsafe}(${user.uuid}) on disable", t)
                }
                BukkitUserManager.unload(user)
            }
        }
        HandlerList.unregisterAll(bootstrap)
        UpdateCheckerMan.shutdown()
        StorageManager.shutdown()
        adventure.close()
        try {
            @OptIn(DelicateCoroutinesApi::class) // Just release the resources
            Dispatchers.shutdown()
        } catch (t: Throwable) {
            err("An exception occurred while shutting down coroutine: $t")
        }
    }

    private fun byPlugMan(): Boolean {
        var found = false
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach {
            val name = it.declaringClass.canonicalName
            if (found || (name != null && name.contains("plugman"))) {
                found = true
            }
        }
        return found
    }

    internal class ServerHotLoadSupport(isHot: Boolean) : HotLoadSupport(isHot) {

        override fun onEnable() {
            super.onEnable()
            loadCriticalClassesBukkit()
        }

        private fun loadCriticalClassesBukkit() {
            InventoryUtils // Plugin onDisable
        }

    }

}