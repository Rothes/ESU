package io.github.rothes.esu.common.command

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.config.EsuLang
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport

object EsuAdminCommand {

    private var reloadHook: Runnable by InitOnce()

    fun register(reloadHook: Runnable) {
        this.reloadHook = reloadHook
        val annotationParser = AnnotationParser(EsuCore.instance.commandManager, User::class.java).installCoroutineSupport()
        annotationParser.manager().parserRegistry().registerParser(ModuleParser.parser())
        annotationParser.parse(EsuAdminCommand)
    }

    @Command("esu reload")
    @Permission("esu.command.admin")
    fun reload(sender: User) {
        EsuConfig.reloadConfig()
        ColorSchemes.reload()
        ModuleManager.reloadModules()
        reloadHook.run()
        // Not sure why EsuLang.get() just don't compile here
        sender.message(EsuLang.instance.get(), { commands.reload.complete })
    }

    @Command("esu module forceEnable <module>")
    @Permission("esu.command.admin")
    fun forceEnable(sender: User, module: Module<*, *>) {
        val tag = unparsed("module-name", module.name)
        if (module.enabled) {
            sender.message(EsuLang.instance.get(), { commands.module.forceEnable.alreadyEnabled }, tag)
        } else if (ModuleManager.forceEnableModule(module)) {
            sender.message(EsuLang.instance.get(), { commands.module.forceEnable.moduleEnabled }, tag)
        } else {
            sender.message(EsuLang.instance.get(), { commands.module.forceEnable.failedEnable }, tag)
        }
    }

    @Command("esu module forceDisable <module>")
    @Permission("esu.command.admin")
    fun forceDisable(sender: User, module: Module<*, *>) {
        val tag = unparsed("module-name", module.name)
        if (!module.enabled) {
            sender.message(EsuLang.instance.get(), { commands.module.forceDisable.alreadyDisabled }, tag)
        } else if (ModuleManager.forceDisableModule(module)) {
            sender.message(EsuLang.instance.get(), { commands.module.forceDisable.moduleDisabled }, tag)
        } else {
            sender.message(EsuLang.instance.get(), { commands.module.forceDisable.failedDisable }, tag)
        }
    }

}