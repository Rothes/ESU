package io.github.rothes.esu.core.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

object ComponentUtils {

    fun fromMiniMessage(miniMessage: String): Component {
        return MiniMessage.miniMessage().deserialize(miniMessage)
    }

    fun fromLegacy(legacyText: String): Component {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText)
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