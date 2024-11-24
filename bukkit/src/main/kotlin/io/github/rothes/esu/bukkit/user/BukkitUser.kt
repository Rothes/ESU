package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.user.User
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

abstract class BukkitUser: User {

    abstract val commandSender: CommandSender

    override var language: String?
        get() = languageUnsafe ?: clientLocale
        set(value) {
            languageUnsafe = value
        }
    override var colorScheme: String?
        get() = colorSchemeUnsafe
        set(value) {
            colorSchemeUnsafe = value
        }

    override val name: String
        get() = commandSender.name

    override fun hasPermission(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }

    override fun message(message: Component) {
        commandSender.sendMessage(message)
    }

}