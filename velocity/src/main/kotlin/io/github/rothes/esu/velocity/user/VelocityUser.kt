package io.github.rothes.esu.velocity.user

import com.velocitypowered.api.command.CommandSource
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.MappedAudience
import io.github.rothes.esu.lib.adventure.audience.Audience

abstract class VelocityUser: User {

    private var _audience: Audience? = null

    abstract override val commandSender: CommandSource

    override val audience: Audience
        get() = _audience ?: MappedAudience(commandSender).also { _audience = it }

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