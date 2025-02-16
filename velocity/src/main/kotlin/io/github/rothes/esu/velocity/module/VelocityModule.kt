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

    protected fun registerCommand(block: VelocityCommandManager<VelocityUser>.() -> Command.Builder<VelocityUser>) {
        with(plugin.commandManager) {
            val command = block.invoke(this).build()
            command(command)
            registeredCommands.add(command)
        }
    }

    protected fun registerCommands(obj: Any, modifier: ((AnnotationParser<VelocityUser>) -> Unit)? = null) {
        with(plugin.commandManager) {
            val annotationParser = AnnotationParser(this, VelocityUser::class.java).installCoroutineSupport()
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

}