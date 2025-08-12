package io.github.rothes.esu.bungee.user

import io.github.rothes.esu.bungee.adventure
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.net.kyori.adventure.audience.Audience
import net.md_5.bungee.api.CommandSender

abstract class BungeeUser: User {

    abstract val commandSender: CommandSender
    override val audience: Audience by lazy {
        adventure.sender(commandSender)
    }

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

    override fun hasPermission(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }

}