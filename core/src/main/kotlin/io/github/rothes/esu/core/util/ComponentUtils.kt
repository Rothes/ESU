package io.github.rothes.esu.core.util

import io.github.rothes.esu.core.config.EsuLocale
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import kotlin.math.floor
import kotlin.time.Duration

object ComponentUtils {

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