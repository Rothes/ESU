package io.github.rothes.esu.velocity.user

import com.velocitypowered.api.command.CommandSource
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.MappedAudience
import io.github.rothes.esu.lib.net.kyori.adventure.audience.Audience

abstract class VelocityUser: User {

    abstract val commandSender: CommandSource
    override val audience: Audience by lazy {
        MappedAudience(commandSender)
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