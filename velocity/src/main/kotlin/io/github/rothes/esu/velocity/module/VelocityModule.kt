package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user.VelocityUser
import org.incendo.cloud.Command
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.velocity.VelocityCommandManager
import kotlin.jvm.java

abstract class VelocityModule<T: ConfigurationPart, L: ConfigurationPart>(
    dataClass: Class<T>, localeClass: Class<L>,
): CommonModule<T, L>(dataClass, localeClass) {

    protected val registeredListeners = arrayListOf<Pair<Any, Any>>()

    override fun disable() {
//        super.disable() // Don't unregister root commands, cloud framework doesn't support it on velocity yet
        for (listener in registeredListeners) {
            unregisterListener(listener.first, listener.second)
        }
        registeredListeners.clear()
    }

    fun registerCommand(block: VelocityCommandManager<VelocityUser>.() -> Command.Builder<VelocityUser>) {
        with(plugin.commandManager) {
            val command = block.invoke(this).build()
            command(command)
            registeredCommands.add(command)
        }
    }

    fun registerCommands(obj: Any, modifier: ((AnnotationParser<VelocityUser>) -> Unit)? = null) {
        with(plugin.commandManager) {
            val annotationParser = AnnotationParser(this, VelocityUser::class.java).installCoroutineSupport()
            annotationParser.registerBuilderModifier(ShortPerm::class.java) { a, b ->
                val perm = if (a.value.isNotEmpty()) "command.${a.value}" else "command"
                b.permission(perm(perm))
            }
            modifier?.invoke(annotationParser)

            val commands = annotationParser.parse(obj)
            registeredCommands.addAll(commands)
        }
    }

    fun registerListener(listener: Any, pluginInstance: Any = plugin.bootstrap) {
        plugin.server.eventManager.register(pluginInstance, listener)
        registeredListeners.add(listener to pluginInstance)
    }

    fun unregisterListener(listener: Any, pluginInstance: Any = plugin.bootstrap) {
        if (pluginInstance === plugin.bootstrap) {
            if (plugin.enabled) {
                plugin.server.eventManager.unregisterListener(pluginInstance, listener)
            }
        } else {
            plugin.server.eventManager.unregisterListener(pluginInstance, listener)
        }
    }

    override fun perm(shortPerm: String): String = "vesu.${name.lowercase()}.${shortPerm.lowercase()}"

}