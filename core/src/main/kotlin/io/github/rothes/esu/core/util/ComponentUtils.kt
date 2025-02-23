package io.github.rothes.esu.core.util

import io.github.rothes.esu.core.config.EsuLocale
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
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

    fun time(epochMilli: Long, key: String = "time"): TagResolver {
        return Formatter.date(key, LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault()))
    }

    fun bytes(bytes: Long, key: String = "bytes"): TagResolver {
        return unparsed(key, when {
            bytes >= 1 shl 30 -> "%.1f GB".format(bytes.toDouble() / (1 shl 30))
            bytes >= 1 shl 20 -> "%.1f MB".format(bytes.toDouble() / (1 shl 20))
            bytes >= 1 shl 10 -> "%.1f KB".format(bytes.toDouble() / (1 shl 10))
            else              -> "$bytes Bytes"
        })
    }

    fun Boolean.enabled(user: User): Component = duel(user, { enabled }) { disabled }
    fun Boolean.on(user: User): Component = duel(user, { on }) { off }
    fun Boolean.yes(user: User): Component = duel(user, { yes }) { no }

    private fun Boolean.duel(user: User, t: EsuLocale.BaseEsuLocaleData.Booleans.() -> String, f: EsuLocale.BaseEsuLocaleData.Booleans.() -> String): Component {
        return user.buildMinimessage(EsuLocale.get(), { if (this@duel) t(booleans) else f(booleans) })
    }

    fun Component.capitalize(): Component {
        return this.replaceText {
            it.match(".+").once()
                .replacement { component ->
                    component.content(component.content().replaceFirstChar { it.titlecase() })
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