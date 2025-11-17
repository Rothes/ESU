package io.github.rothes.esu.core.util

import io.github.rothes.esu.core.config.EsuLang
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.ComponentLike
import io.github.rothes.esu.lib.adventure.text.flattener.ComponentFlattener
import io.github.rothes.esu.lib.adventure.text.format.TextDecoration
import io.github.rothes.esu.lib.adventure.text.minimessage.MiniMessage
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.Tag
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.Formatter
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.Placeholder
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.adventure.text.serializer.legacy.LegacyComponentSerializer
import io.github.rothes.esu.lib.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object ComponentUtils {

    private var legacySerializer = LegacyComponentSerializer.legacySection()
    private var plainTextSerializer = PlainTextComponentSerializer.plainText()

    var flattener = ComponentFlattener.basic()
        set(value) {
            field = value
            legacySerializer = LegacyComponentSerializer.builder().flattener(value).build()
            plainTextSerializer = PlainTextComponentSerializer.builder().flattener(value).build()
            LogUser.setFlattener(value)
        }

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
        get() = if (isEmpty()) this else buildString(this.length) {
            var last = this@legacyColorCharParsed[0]
            var i = 1
            while (i < this@legacyColorCharParsed.length) {
                if (last == '&') {
                    var parsed = true
                    when (this@legacyColorCharParsed[i]) {
                        '0' -> append("<black>")
                        '1' -> append("<dark_blue>")
                        '2' -> append("<dark_green>")
                        '3' -> append("<dark_aqua>")
                        '4' -> append("<dark_red>")
                        '5' -> append("<dark_purple>")
                        '6' -> append("<gold>")
                        '7' -> append("<gray>")
                        '8' -> append("<dark_gray>")
                        '9' -> append("<blue>")
                        'A', 'a' -> append("<green>")
                        'B', 'b' -> append("<aqua>")
                        'C', 'c' -> append("<red>")
                        'D', 'd' -> append("<light_purple>")
                        'E', 'e' -> append("<yellow>")
                        'F', 'f' -> append("<white>")
                        'K', 'k' -> append("<obf>")
                        'L', 'l' -> append("<b>")
                        'M', 'm' -> append("<st>")
                        'N', 'n' -> append("<u>")
                        'O', 'o' -> append("<i>")
                        'R', 'r' -> append("<reset>")
                        else -> {
                            append(last)
                            parsed = false
                        }
                    }
                    if (parsed) i++
                } else {
                    append(last)
                }
                last = this@legacyColorCharParsed[i++]
            }
            append(last)
        }

    val Component.legacy
        get() = legacySerializer.serialize(this)

    val Component.plainText
        get() = plainTextSerializer.serialize(this)

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

    fun durationTicks(ticks: Long, user: User, key: String = "duration"): TagResolver.Single {
        return durationMillis(ticks * 20, user, key)
    }
    fun durationMillis(millis: Long, user: User, key: String = "duration"): TagResolver.Single {
        return duration(millis.milliseconds, user, key)
    }
    fun duration(duration: Duration, user: User, key: String = "duration"): TagResolver.Single {
        return Placeholder.parsed(key, duration.toComponents { d, h, m, s, ns ->
            buildString {
                val separator = user.localed(EsuLang.get()) { format.duration.separator }
                val append = fun(block: EsuLang.BaseEsuLangData.() -> String?, v: Number) {
                    append(user.localed(EsuLang.get(), block).replace("<value>", v.toString()))
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

    private fun Boolean.duel(user: User, t: EsuLang.BaseEsuLangData.Booleans.() -> String, f: EsuLang.BaseEsuLangData.Booleans.() -> String): Component {
        return user.buildMiniMessage(EsuLang.get(), { if (this@duel) t(booleans) else f(booleans) })
    }

    fun Component.capitalize(): Component {
        return this.replaceText {
            it.match(".+").once()
                .replacement { component ->
                    component.content(component.content().replaceFirstChar { it.titlecase() })
                }
        }
    }

    fun <T> pLang(viewer: User,
                                     locales: MultiLangConfiguration<T>, block: T.() -> Map<String, String>?,
                                     vararg params: TagResolver): TagResolver {
        val langMap = viewer.localed(locales, block)
        return pLang(viewer, langMap, params = params)
    }

    fun pLang(viewer: User, placeholders: Map<String, String>, vararg params: TagResolver): TagResolver {
        return TagResolver.resolver(setOf("pl", "placeholder_lang")) { arg, _ ->
            val pop = arg.popOr("One argument required for placeholder_lang")
            val key = pop.value()
            val placeholder = placeholders[key]
            if (placeholder != null)
                Tag.selfClosingInserting(
                    viewer.buildMiniMessage(placeholder, params = params)
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