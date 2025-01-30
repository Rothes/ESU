package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.configuration.pojo.MessageData
import io.github.rothes.esu.core.configuration.pojo.SoundData
import io.github.rothes.esu.core.configuration.pojo.TitleData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.*
import kotlin.experimental.ExperimentalTypeInference

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

    val isOnline: Boolean

    fun hasPermission(permission: String): Boolean

    fun <T: ConfigurationPart, R> localedOrNull(locales: MultiLocaleConfiguration<T>, block: T.() -> R?): R? {
        return locales.get(language, block)
    }

    fun <T: ConfigurationPart, R> localed(locales: MultiLocaleConfiguration<T>, block: T.() -> R?): R {
        return localedOrNull(locales, block) ?: throw NullPointerException()
    }

    @OptIn(ExperimentalTypeInference::class)
    @OverloadResolutionByLambdaReturnType
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("sendMessage")
    fun <T: ConfigurationPart> message(locales: MultiLocaleConfiguration<T>, block: T.() -> MessageData?, vararg params: TagResolver) {
        val messageData = localed(locales, block)
        message(messageData, params = params)
    }

    fun <T: ConfigurationPart> message(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        message(buildMinimessage(locales, block, params = params))
    }
    fun minimessage(message: String, vararg params: TagResolver) {
        message(buildMinimessage(message, params = params))
    }

    fun <T: ConfigurationPart> buildMinimessage(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver): Component {
        return buildMinimessage(localed(locales, block), params = params)
    }
    fun buildMinimessage(message: String, vararg params: TagResolver): Component {
        return MiniMessage.miniMessage().deserialize(message, *params,
            ColorSchemes.schemes.get(colorScheme) { tagResolver }!!
        )
    }

    fun message(messageData: MessageData, vararg params: TagResolver) {
        with(messageData) {
            if (chat != null) {
                message(buildMinimessage(chat, params = params))
            }
            if (actionBar != null) {
                actionBar(buildMinimessage(actionBar, params = params))
            }
            if (title != null) {
                title(title, params = params)
            }
            if (sound != null) {
                playSound(sound)
            }
        }
    }

    fun message(message: String) {
        message(LegacyComponentSerializer.legacySection().deserialize(message))
    }
    fun message(message: Component)

    fun <T: ConfigurationPart> kick(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver)

    fun actionBar(message: Component)
    fun title(title: TitleData, vararg params: TagResolver)
    fun playSound(sound: SoundData)

}