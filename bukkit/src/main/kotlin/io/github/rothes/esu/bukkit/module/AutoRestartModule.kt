package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.common.module.AbstractAutoRestartModule
import io.github.rothes.esu.core.user.User
import org.bukkit.Bukkit

object AutoRestartModule: AbstractAutoRestartModule() {

    override val consoleUser: User = ConsoleUser
    override val rootCommand: String = "autoRestart"
    override val rootCommandAlias: String = "ar"

    override fun runCommands() {
        Scheduler.global(2) { // Make sure all players are disconnected on their region thread
            config.commands.forEach {
                Bukkit.dispatchCommand(ConsoleUser.commandSender, it)
            }
        }
    }

}