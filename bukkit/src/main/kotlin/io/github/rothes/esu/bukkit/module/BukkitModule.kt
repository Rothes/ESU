package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import org.incendo.cloud.Command
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.bukkit.BukkitCommandManager
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport

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
    @JvmOverloads
    protected fun registerCommands(obj: Any, modifier: ((AnnotationParser<BukkitUser>) -> Unit)? = null) {
        with(plugin.commandManager) {
            val annotationParser = AnnotationParser(this, BukkitUser::class.java).installCoroutineSupport()
            annotationParser.registerBuilderModifier(ShortPerm::class.java) { a, b ->
                val value = perm(a.value)
                val perm = if (value.isNotEmpty()) "command.$value" else "command"
                b.permission(perm)
            }
            modifier?.invoke(annotationParser)

            val commands = annotationParser.parse(obj)
            registeredCommands.addAll(commands)
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