package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.user.User
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

abstract class BukkitUser: User {

    abstract val commandSender: CommandSender

    override val name: String
        get() = commandSender.name

    override fun hasPermission(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }

    override fun message(message: Component) {
        commandSender.sendMessage(message)
    }

}