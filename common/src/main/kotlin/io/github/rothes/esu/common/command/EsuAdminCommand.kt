package io.github.rothes.esu.common.command

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.config.EsuLang
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.ArgumentMode
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.SyntaxFragment
import org.incendo.cloud.annotations.descriptor.ImmutableCommandDescriptor
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport

object EsuAdminCommand {

    private var reloadHook: Runnable by InitOnce()

    fun register(reloadHook: Runnable) {
        this.reloadHook = reloadHook

        val rootCmd = EsuCore.instance.basePermissionNode
        val annotationParser = AnnotationParser(EsuCore.instance.commandManager, User::class.java).installCoroutineSupport()

        val sp = annotationParser.commandExtractor()
        annotationParser.commandExtractor {
            sp.extractCommands(it).map { desc ->
                val syntax = buildList(desc.syntax().size + 1) {
                    add(SyntaxFragment(rootCmd, listOf(), ArgumentMode.LITERAL))
                    addAll(desc.syntax())
                }
                ImmutableCommandDescriptor.of(desc.method(), rootCmd, syntax, rootCmd, desc.requiredSender())
            }
        }
        annotationParser.registerBuilderDecorator {
            it.permission("$rootCmd.command.admin")
        }

        annotationParser.manager().parserRegistry().registerParser(ModuleParser.parser())
        annotationParser.parse(EsuAdminCommand)
    }

    @Command("reload")
    fun reload(sender: User) {
        EsuConfig.reloadConfig()
        ColorSchemes.reload()
        ModuleManager.reloadModules()
        reloadHook.run()
        val prefix = component("prefix", sender.buildMiniMessage(EsuLang.get(), { commands.prefix }))
        // Not sure why EsuLang.get() just don't compile here
        sender.message(EsuLang.instance.get(), { commands.reload.complete }, prefix)
    }

    @Command("module forceEnable <module>")
    fun forceEnable(sender: User, module: Module<*, *>) {
        val tag = unparsed("module-name", module.name)
        val prefix = component("prefix", sender.buildMiniMessage(EsuLang.get(), { commands.prefix }))
        if (module.enabled) {
            sender.message(EsuLang.instance.get(), { commands.module.forceEnable.alreadyEnabled }, tag, prefix)
        } else if (ModuleManager.forceEnableModule(module)) {
            sender.message(EsuLang.instance.get(), { commands.module.forceEnable.moduleEnabled }, tag, prefix)
        } else {
            sender.message(EsuLang.instance.get(), { commands.module.forceEnable.failedEnable }, tag, prefix)
        }
    }

    @Command("module forceDisable <module>")
    fun forceDisable(sender: User, module: Module<*, *>) {
        val tag = unparsed("module-name", module.name)
        val prefix = component("prefix", sender.buildMiniMessage(EsuLang.get(), { commands.prefix }))
        if (!module.enabled) {
            sender.message(EsuLang.instance.get(), { commands.module.forceDisable.alreadyDisabled }, tag, prefix)
        } else if (ModuleManager.forceDisableModule(module)) {
            sender.message(EsuLang.instance.get(), { commands.module.forceDisable.moduleDisabled }, tag, prefix)
        } else {
            sender.message(EsuLang.instance.get(), { commands.module.forceDisable.failedDisable }, tag, prefix)
        }
    }

}