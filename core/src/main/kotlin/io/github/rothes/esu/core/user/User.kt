package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.*

interface User {

    val name: String
    val nameUnsafe: String?
    val clientLocale: String?
    val uuid: UUID
    val dbId: Int

    var language: String?
    var colorScheme: String?

    var languageUnsafe: String?
    var colorSchemeUnsafe: String?

    fun hasPermission(permission: String): Boolean

    fun <T: ConfigurationPart, R> localedOrNull(locales: MultiLocaleConfiguration<T>, block: T.() -> R?): R? {
        return locales.get(language, block)
    }

    fun <T: ConfigurationPart, R> localed(locales: MultiLocaleConfiguration<T>, block: T.() -> R?): R {
        return localedOrNull(locales, block) ?: throw NullPointerException()
    }

    fun <T: ConfigurationPart> message(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        minimessage(localed(locales, block), params = params)
    }
    fun minimessage(message: String, vararg params: TagResolver) {
        message(MiniMessage.miniMessage().deserialize(message, *params,
            ColorSchemes.schemes.get(colorScheme) { tagResolver }!!))
    }
    fun message(message: String) {
        message(LegacyComponentSerializer.legacySection().deserialize(message))
    }
    fun message(message: Component)

    fun <T: ConfigurationPart> kick(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver)

}