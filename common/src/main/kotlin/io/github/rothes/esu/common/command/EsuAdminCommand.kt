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

        val rootCmd = EsuCore.instance.baseCommandNode
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