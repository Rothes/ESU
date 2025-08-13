package io.github.rothes.esu.core.util

import io.github.rothes.esu.core.config.EsuLocale
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import io.github.rothes.esu.lib.net.kyori.adventure.text.ComponentLike
import io.github.rothes.esu.lib.net.kyori.adventure.text.format.TextDecoration
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.MiniMessage
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.Tag
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.Formatter
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import io.github.rothes.esu.lib.net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.floor
import kotlin.time.Duration

object ComponentUtils {

    val capitalize = TagResolver.resolver("capitalize") { arg, context ->
        val deserialize = context.deserialize(arg.popOr("One argument expected for capitalize").value())
        Tag.inserting(deserialize.capitalize())
    }

    fun fromLegacy(legacyText: String): Component {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText)
    }

    fun fromMiniMessage(miniMessage: String): Component {
        return MiniMessage.miniMessage().deserialize(miniMessage)
    }

    val String.miniMessage
        get() = fromMiniMessage(this)

    val String.legacyColorCharParsed
        get() = replace("&0", "<black>")
        .replace("&1", "<dark_blue>")
        .replace("&2", "<dark_green>")
        .replace("&3", "<dark_aqua>")
        .replace("&4", "<dark_red>")
        .replace("&5", "<dark_purple>")
        .replace("&6", "<gold>")
        .replace("&7", "<gray>")
        .replace("&8", "<dark_gray>")
        .replace("&9", "<blue>")
        .replace("&[Aa]".toRegex(), "<green>")
        .replace("&[Bb]".toRegex(), "<aqua>")
        .replace("&[Cc]".toRegex(), "<red>")
        .replace("&[Dd]".toRegex(), "<light_purple>")
        .replace("&[Ee]".toRegex(), "<yellow>")
        .replace("&[Ff]".toRegex(), "<white>")
        .replace("&[Kk]".toRegex(), "<obf>")
        .replace("&[Ll]".toRegex(), "<b>")
        .replace("&[Mm]".toRegex(), "<st>")
        .replace("&[Nn]".toRegex(), "<u>")
        .replace("&[Oo]".toRegex(), "<i>")
        .replace("&[Rr]".toRegex(), "<reset>")

    val Component.legacy
        get() = LegacyComponentSerializer.legacySection().serialize(this)

    val Component.plainText
        get() = PlainTextComponentSerializer.plainText().serialize(this)

    val Component.nonItalic
        get() = decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)


    fun unparsed(key: String, value: Any?): TagResolver.Single {
        return Placeholder.unparsed(key, value.toString())
    }

    fun parsed(key: String, value: Any?): TagResolver.Single {
        return Placeholder.parsed(key, value.toString())
    }

    fun component(key: String, component: ComponentLike): TagResolver.Single {
        return Placeholder.component(key, component)
    }

    fun duration(duration: Duration, user: User, key: String = "duration"): TagResolver.Single {
        return Placeholder.parsed(key, duration.toComponents { d, h, m, s, ns ->
            buildString {
                val separator = user.localed(EsuLocale.get()) { format.duration.separator }
                val append = fun(block: EsuLocale.BaseEsuLocaleData.() -> String?, v: Number) {
                    append(user.localed(EsuLocale.get(), block).replace("<value>", v.toString()))
                    append(separator)
                }
                if (d > 0) append({ format.duration.day }, d)
                if (h > 0) append({ format.duration.hour }, h)
                if (m > 0) append({ format.duration.minute }, m)
                if (s > 0) append({ format.duration.second }, s + floor(ns / 1_000_000_000.0).toLong())
                if (isEmpty()) append({ format.duration.millis }, ns / 1_000_000)
                for (i in 0 ..< separator.length) {
                    deleteAt(length - 1)
                }
            }
        })
    }

    fun time(epochMilli: Long = System.currentTimeMillis(), key: String = "time"): TagResolver {
        return Formatter.date(key, LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault()))
    }

    fun amount(amount: Long, key: String = "amount",
               g: String = " G", m: String = " M", k: String = " K", b: String = ""): TagResolver {
        return unparsed(key, when {
            amount >= 1_000_000_000 -> "%.1f$g".format(amount.toDouble() / 1_000_000_000)
            amount >= 1_000_000     -> "%.1f$m".format(amount.toDouble() / 1_000_000)
            amount >= 1_000         -> "%.1f$k".format(amount.toDouble() / 1_000)
            else                    -> "$amount$b"
        })
    }

    fun bytes(bytes: Long, key: String = "bytes",
              gb: String = " GiB", mb: String = " MiB", kb: String = " KiB", b: String = " Bytes"): TagResolver {
        return unparsed(key, when {
            bytes >= 1 shl 30 -> "%.1f$gb".format(bytes.toDouble() / (1 shl 30))
            bytes >= 1 shl 20 -> "%.1f$mb".format(bytes.toDouble() / (1 shl 20))
            bytes >= 1 shl 10 -> "%.1f$kb".format(bytes.toDouble() / (1 shl 10))
            else              -> "$bytes$b"
        })
    }

    fun Boolean.enabled(user: User): Component = duel(user, { enabled }) { disabled }
    fun Boolean.on(user: User): Component = duel(user, { on }) { off }
    fun Boolean.yes(user: User): Component = duel(user, { yes }) { no }

    private fun Boolean.duel(user: User, t: EsuLocale.BaseEsuLocaleData.Booleans.() -> String, f: EsuLocale.BaseEsuLocaleData.Booleans.() -> String): Component {
        return user.buildMiniMessage(EsuLocale.get(), { if (this@duel) t(booleans) else f(booleans) })
    }

    fun Component.capitalize(): Component {
        return this.replaceText {
            it.match(".+").once()
                .replacement { component ->
                    component.content(component.content().replaceFirstChar { it.titlecase() })
                }
        }
    }

    fun <T: ConfigurationPart> pLang(viewer: User,
                                     locales: MultiLocaleConfiguration<T>, block: T.() -> Map<String, String>?,
                                     vararg params: TagResolver): TagResolver {
        val langMap = viewer.localed(locales, block)
        return pLang(viewer, langMap, params = params)
    }

    fun pLang(viewer: User, langMap: Map<String, String>, vararg params: TagResolver): TagResolver {
        return TagResolver.resolver(setOf("pl", "placeholder_lang")) { arg, context ->
            val pop = arg.popOr("One argument required for placeholder_lang")
            val key = pop.value()
            val localed = viewer.localedOrNull(langMap)
            if (localed != null)
                Tag.selfClosingInserting(
                    viewer.buildMiniMessage(localed, params = params)
                )
            else {
                Tag.selfClosingInserting(
                    Component.text("lang.$key")
                )
            }
        }
    }

//    fun Component.set(vararg objects: Any): Component {
//        this.replaceText()
//        Component.text().append(this).mapChildrenDeep { component ->
//            val builder = component.toBuilder()
//            component.clickEvent()?.let { clickEvent ->
//                builder.clickEvent(ClickEvent.clickEvent(clickEvent.action(), clickEvent.value()))
//            }
//            component.hoverEvent()?.let { hoverEvent ->
//                if (hoverEvent.value() is Component) {
//                    hoverEvent.value()
//                }
//                when (hoverEvent.action()) {
//                    HoverEvent.Action.SHOW_TEXT -> builder.hoverEvent(hoverEvent.value(Component.text().build()))
//                }
//                builder.hoverEvent(hoverEvent.value(""))
//            }
//            if (component.clickEvent() != null) {
//                builder.clickEvent(component.clickEvent())
//            }
//            if (builder.clickEvent() != null) {
//                builder.clickEvent()
//            }
//            builder.build()
//        }
//        this as BuildableComponent
//    }

}