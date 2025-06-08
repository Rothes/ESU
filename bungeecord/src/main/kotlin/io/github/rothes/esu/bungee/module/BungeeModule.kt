package io.github.rothes.esu.bungee.module

import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.bungee.plugin
import io.github.rothes.esu.bungee.user.BungeeUser
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.Command
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport

abstract class BungeeModule<T: ConfigurationPart, L: ConfigurationPart>(
    dataClass: Class<T>, localeClass: Class<L>,
): CommonModule<T, L>(dataClass, localeClass) {

    protected val registeredListeners = arrayListOf<Pair<Listener, Plugin>>()

    override fun disable() {
//        super.disable() // Don't unregister root commands, cloud framework doesn't support it on velocity yet
        for (listener in registeredListeners) {
            unregisterListener(listener.first, listener.second)
        }
        registeredListeners.clear()
    }

    fun registerCommand(block: BungeeCommandManager<BungeeUser>.() -> Command.Builder<BungeeUser>) {
        with(plugin.commandManager) {
            val command = block.invoke(this).build()
            command(command)
            registeredCommands.add(command)
        }
    }

    fun registerCommands(obj: Any, modifier: ((AnnotationParser<BungeeUser>) -> Unit)? = null) {
        with(plugin.commandManager) {
            val annotationParser = AnnotationParser(this, BungeeUser::class.java).installCoroutineSupport()
            annotationParser.registerBuilderModifier(ShortPerm::class.java) { a, b ->
                val perm = if (a.value.isNotEmpty()) "command.${a.value}" else "command"
                b.permission(perm(perm))
            }
            modifier?.invoke(annotationParser)

            val commands = annotationParser.parse(obj)
            registeredCommands.addAll(commands)
        }
    }

    fun registerListener(listener: Listener, pluginInstance: Plugin = plugin) {
        ProxyServer.getInstance().pluginManager.registerListener(pluginInstance, listener)
        registeredListeners.add(listener to pluginInstance)
    }

    fun unregisterListener(listener: Listener, pluginInstance: Plugin = plugin) {
        if (pluginInstance === plugin) {
            if (pluginInstance.enabled) {
                ProxyServer.getInstance().pluginManager.unregisterListener(listener)
            }
        } else {
            ProxyServer.getInstance().pluginManager.unregisterListener(listener)
        }
    }

}