package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.minimessage.MiniMessage
import java.lang.reflect.Type
import java.util.function.Predicate

object ComponentSerializer: ScalarSerializer<Component>(Component::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Component {
        return MiniMessage.miniMessage().deserialize(obj.toString())
    }

    override fun serialize(component: Component, typeSupported: Predicate<Class<*>>): String {
        return MiniMessage.miniMessage().serialize(component)
    }

}
