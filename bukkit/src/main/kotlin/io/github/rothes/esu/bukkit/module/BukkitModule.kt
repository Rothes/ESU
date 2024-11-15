package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import org.incendo.cloud.Command
import org.incendo.cloud.bukkit.BukkitCommandManager

abstract class BukkitModule<T: ConfigurationPart, L: ConfigurationPart>(
    dataClass: Class<T>, localeClass: Class<L>,
): CommonModule<T, L>(dataClass, localeClass) {

    private val registeredCommands = LinkedHashSet<Command<BukkitUser>>()

    @Deprecated("Use registerCommand instead")
    protected fun regCmd(block: BukkitCommandManager<BukkitUser>.() -> Command.Builder<BukkitUser>) {
        registerCommand(block)
    }

    protected fun registerCommand(block: BukkitCommandManager<BukkitUser>.() -> Command.Builder<BukkitUser>) {
        with(plugin.commandManager) {
            val command = block.invoke(this).build()
            command(command)
            registeredCommands.add(command)
        }
    }
    protected fun unregisterCommands() {
        with(plugin.commandManager) {
            registeredCommands.forEach {
                deleteRootCommand(it.rootComponent().name())
            }
            registeredCommands.clear()
        }
    }

    override fun disable() {
        unregisterCommands()
    }
}