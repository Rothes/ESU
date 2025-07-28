package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.storage.StorageManager.UsersTable.language
import io.github.rothes.esu.core.util.ComponentUtils.capitalize
import io.github.rothes.esu.core.util.ComponentUtils.legacyColorCharParsed
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.*
import kotlin.collections.get
import kotlin.experimental.ExperimentalTypeInference
import kotlin.text.startsWith

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

    val colorSchemeInstance
        get() = ColorSchemes.schemes.get(colorScheme) { tagResolver }!!

    fun hasPermission(permission: String): Boolean

    fun <V, R> localedOrNull(langMap: Map<String, V>, block: (V) -> R?): R? {
        val lang = language
        return langMap[lang]?.let(block)
        // If this locale is not found, try the same language.
            ?: lang?.split('_')?.get(0)?.let { language ->
                val lang = language + '_'
                langMap.entries.filter { it.key.startsWith(lang) }.firstNotNullOfOrNull { block(it.value) }
            }
            // Still? Use the server default locale instead.
            ?: langMap[EsuConfig.get().locale]?.let(block)
            // Use the default value.
            ?: langMap["en_us"]?.let(block)
            // Maybe it doesn't provide en_us locale...?
            ?: langMap.values.firstNotNullOfOrNull { block(it) }
    }

    fun <V> localedOrNull(langMap: Map<String, V>): V? {
        return localedOrNull(langMap) { it }
    }

    fun <T: ConfigurationPart, R> localedOrNull(locales: MultiLocaleConfiguration<T>, block: T.() -> R?): R? {
        return localedOrNull(locales.configs, block)
    }

    fun <V, R> localed(langMap: Map<String, V>, block: (V) -> R?): R {
        return localedOrNull(langMap, block) ?: throw NullPointerException()
    }

    fun <V> localed(langMap: Map<String, V>): V {
        return localed(langMap) { it }
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
        return MiniMessage.miniMessage().deserialize(
            message.let {
                if (EsuConfig.get().legacyColorChar)
                    it.legacyColorCharParsed
                else
                    it
            },
            *params, colorSchemeInstance, capitalize
        )
    }

    fun message(messageData: MessageData, vararg params: TagResolver) {
        message(messageData.parsed(this, params = params))
    }

    fun message(messageData: ParsedMessageData) {
        with(messageData) {
            chat?.let { it.forEach { msg -> message(msg) } }
            actionBar?.let { actionBar(it) }
            title?.let { title(it) }
            sound?.let { playSound(it) }
        }
    }

    fun message(message: String) {
        message(LegacyComponentSerializer.legacySection().deserialize(message))
    }
    fun message(message: Component)

    fun <T: ConfigurationPart> kick(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver)

    fun actionBar(message: Component)
    fun title(title: ParsedMessageData.ParsedTitleData)
    fun playSound(sound: SoundData)

    fun clearTitle()
    fun clearActionBar() {
        actionBar(Component.empty())
    }

}