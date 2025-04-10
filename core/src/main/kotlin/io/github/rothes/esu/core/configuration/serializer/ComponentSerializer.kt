package io.github.rothes.esu.core.configuration.serializer

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
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
