package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.Command
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.bukkit.BukkitCommandManager
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport

abstract class BukkitModule<C: ConfigurationPart, L: ConfigurationPart> : CommonModule<C, L>() {

    protected val registeredListeners = linkedSetOf<Listener>()

    override fun disable() {
        unregisterListeners()
        super.disable()
    }

    fun registerCommand(block: BukkitCommandManager<BukkitUser>.() -> Command.Builder<BukkitUser>) {
        with(plugin.commandManager) {
            val command = block.invoke(this).build()
            command(command)
            registeredCommands.add(command)
        }
    }

    fun registerCommands(obj: Any, modifier: ((AnnotationParser<BukkitUser>) -> Unit)? = null) {
        with(plugin.commandManager) {
            val annotationParser = AnnotationParser(this, BukkitUser::class.java).installCoroutineSupport()
            annotationParser.registerBuilderModifier(ShortPerm::class.java) { a, b ->
                val perm = if (a.value.isNotEmpty()) "command.${a.value}" else "command"
                b.permission(perm(perm))
            }
            modifier?.invoke(annotationParser)

            val commands = annotationParser.parse(obj)
            registeredCommands.addAll(commands)
        }
    }

    fun registerListener(listener: Listener, plugin: JavaPlugin = io.github.rothes.esu.bukkit.bootstrap) {
        Bukkit.getPluginManager().registerEvents(listener, plugin)
        registeredListeners.add(listener)
    }

    fun unregisterListeners() {
        registeredListeners.removeIf {
            HandlerList.unregisterAll(it)
            true
        }
    }

    fun log(msg: String) {
        ConsoleUser.log("[$name] $msg]")
    }

}