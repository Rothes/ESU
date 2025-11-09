package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.common.module.AbstractAutoRestartModule
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user.ConsoleUser

object AutoRestartModule: AbstractAutoRestartModule() {

    override val consoleUser: User = ConsoleUser
    override val rootCommand: String = "vAutoRestart"
    override val rootCommandAlias: String = "var"

    override fun runCommands() {
        config.commands.forEach {
            plugin.server.commandManager.executeAsync(ConsoleUser.commandSender, it)
        }
    }
}