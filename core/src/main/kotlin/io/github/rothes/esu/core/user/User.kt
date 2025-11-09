package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils
import io.github.rothes.esu.core.util.ComponentUtils.capitalize
import io.github.rothes.esu.core.util.ComponentUtils.legacyColorCharParsed
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.inventory.Book
import io.github.rothes.esu.lib.adventure.sound.Sound
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.minimessage.MiniMessage
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.adventure.title.Title
import io.github.rothes.esu.lib.adventure.title.TitlePart
import java.util.*
import kotlin.experimental.ExperimentalTypeInference

interface User {

    val commandSender: Any
    val audience: Audience

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

    val colorSchemeTagInstance
        get() = ColorSchemes.schemes.get(colorScheme) { this }!!
    val colorSchemeTagResolver
        get() = colorSchemeTagInstance.tagResolver

    fun getTagResolvers(): Iterable<TagResolver> {
        return DEFAULT_TAG_RESOLVERS
    }

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

    fun <T, R> localedOrNull(locales: MultiLangConfiguration<T>, block: T.() -> R?): R? {
        return localedOrNull(locales.configs, block)
    }

    fun <V, R> localed(langMap: Map<String, V>, block: (V) -> R?): R {
        return localedOrNull(langMap, block) ?: throw NullPointerException()
    }

    fun <V> localed(langMap: Map<String, V>): V {
        return localed(langMap) { it }
    }

    fun <T, R> localed(locales: MultiLangConfiguration<T>, block: T.() -> R?): R {
        return localedOrNull(locales, block) ?: throw NullPointerException()
    }

    @OptIn(ExperimentalTypeInference::class)
    @OverloadResolutionByLambdaReturnType
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("sendMessage")
    fun <T> message(locales: MultiLangConfiguration<T>, block: T.() -> MessageData?, vararg params: TagResolver) {
        val messageData = localed(locales, block)
        message(messageData, params = params)
    }

    fun <T> message(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        message(buildMiniMessage(locales, block, params = params))
    }
    fun miniMessage(message: String, vararg params: TagResolver) {
        message(buildMiniMessage(message, params = params))
    }

    fun <T> buildMiniMessage(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver): Component {
        return buildMiniMessage(localed(locales, block), params = params)
    }
    fun buildMiniMessage(message: String, vararg params: TagResolver): Component {
        return MiniMessage.miniMessage().deserialize(
            message.let {
                if (EsuConfig.get().legacyColorChar)
                    it.legacyColorCharParsed
                else
                    it
            },
            TagResolver.builder()
                .resolvers(getTagResolvers())
                .resolvers(colorSchemeTagResolver)
                .resolvers(*params)
                .build()
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
        message(ComponentUtils.fromLegacy(message))
    }
    fun message(message: Component) {
        audience.sendMessage(message)
    }

    fun <T> kick(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver)

    fun actionBar(message: Component) {
        audience.sendActionBar(message)
    }
    fun title(title: ParsedMessageData.ParsedTitleData) {
        val mainTitle = title.title
        val subTitle = title.subTitle
        val times = title.times

        if (mainTitle != null && subTitle != null) {
            audience.showTitle(Title.title(mainTitle, subTitle, times?.adventure))
        } else {
            if (times != null) {
                audience.sendTitlePart(TitlePart.TIMES, times.adventure)
            }
            if (mainTitle != null) {
                audience.sendTitlePart(TitlePart.TITLE, mainTitle)
            }
            if (subTitle != null) {
                audience.sendTitlePart(TitlePart.SUBTITLE, subTitle)
            }
        }
    }
    fun playSound(sound: SoundData) {
        audience.playSound(sound.adventure, Sound.Emitter.self())
    }

    fun clearTitle() {
        audience.clearTitle()
    }
    fun clearActionBar() {
        actionBar(Component.empty())
    }

    fun openBook(book: Book.Builder) {
        openBook(book.build())
    }

    fun openBook(book: Book) {
        audience.openBook(book)
    }

    // Server Adventure functions
    fun message(message: net.kyori.adventure.text.Component) {
        message(message.esu)
    }
    fun actionBar(message: net.kyori.adventure.text.Component) {
        actionBar(message.esu)
    }

    companion object {
        val DEFAULT_TAG_RESOLVERS = listOf(capitalize)
    }

}