/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils.capitalize
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.core.util.ComponentUtils.legacyColorCharParsed
import io.github.rothes.esu.core.util.lang.LangUtils.getLangOrNull
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
    val dbName: String?

    var dbDirty: Boolean

    var language: String?
    var colorScheme: String?

    var languageUnsafe: String?
    var colorSchemeUnsafe: String?

    val isOnline: Boolean

    val colorSchemeInstance
        get() = ColorSchemes.schemes.get(colorScheme) { this }!!
    val colorSchemeTagResolver
        get() = colorSchemeInstance.tagResolver

    fun getTagResolvers(): Iterable<TagResolver> {
        return DEFAULT_TAG_RESOLVERS
    }

    fun hasPermission(permission: String): Boolean

    fun <V, R> langOrNull(lang: Map<String, V>, transform: (V) -> R?): R? {
        return lang.getLangOrNull(language, transform)
    }

    fun <V> langOrNull(lang: Map<String, V>): V? {
        return langOrNull(lang) { it }
    }

    fun <T, R> langOrNull(lang: MultiLangConfiguration<T>, transform: T.() -> R?): R? {
        return langOrNull(lang.configs, transform)
    }

    fun <V, R> lang(lang: Map<String, V>, transform: (V) -> R?): R {
        return langOrNull(lang, transform) ?: throw NullPointerException()
    }

    fun <V> lang(langMap: Map<String, V>): V {
        return lang(langMap) { it }
    }

    fun <T, R> lang(lang: MultiLangConfiguration<T>, transform: T.() -> R?): R {
        return langOrNull(lang, transform) ?: throw NullPointerException()
    }

    /*  Legacy language functions  */
    @Deprecated("Use #lang") fun <V, R> localedOrNull(langMap: Map<String, V>, block: (V) -> R?): R? = langOrNull(langMap, block)
    @Deprecated("Use #lang") fun <V> localedOrNull(langMap: Map<String, V>): V? = langOrNull(langMap)
    @Deprecated("Use #lang") fun <T, R> localedOrNull(locales: MultiLangConfiguration<T>, block: T.() -> R?): R? = langOrNull(locales, block)
    @Deprecated("Use #lang") fun <V, R> localed(langMap: Map<String, V>, block: (V) -> R?): R = lang(langMap, block)
    @Deprecated("Use #lang") fun <V> localed(langMap: Map<String, V>): V = lang(langMap)
    @Deprecated("Use #lang") fun <T, R> localed(locales: MultiLangConfiguration<T>, block: T.() -> R?): R = lang(locales, block)
    /*  Legacy language functions  */

    @OptIn(ExperimentalTypeInference::class)
    @OverloadResolutionByLambdaReturnType
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("sendMessage")
    fun <T> message(locales: MultiLangConfiguration<T>, block: T.() -> MessageData?, vararg params: TagResolver) {
        val messageData = lang(locales, block)
        message(messageData, params = params)
    }

    fun <T> message(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        message(buildMiniMessage(locales, block, params = params))
    }
    fun miniMessage(message: String, vararg params: TagResolver) {
        message(buildMiniMessage(message, params = params))
    }

    fun <T> buildMiniMessage(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver): Component {
        return buildMiniMessage(lang(locales, block), params = params)
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
        message(message.legacy)
    }
    fun message(message: Component) {
        audience.sendMessage(message)
    }

    fun <T> kick(lang: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver)

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