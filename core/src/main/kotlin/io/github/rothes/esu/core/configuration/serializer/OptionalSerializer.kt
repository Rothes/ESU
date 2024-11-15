package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.core.EsuCore
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

object OptionalSerializer: TypeSerializer<Optional<*>> {

    const val DISABLED = "disabled"

    override fun deserialize(type: Type, node: ConfigurationNode): Optional<*> {
        if (type !is ParameterizedType) {
            throw SerializationException(type, "Raw types are not supported for optional")
        }
        if (type.actualTypeArguments.size != 1) {
            throw SerializationException(type, "Optional expected a type argument!")
        }
        val value = type.actualTypeArguments[0]
        val serializer = node.options().serializers()[value] ?: throw SerializationException(
            type, "No type serializer available for optional type $value"
        )
        if (node.raw() == DISABLED) {
            return Optional.empty<Any>()
        }
        try {
            return Optional.ofNullable<Any>(serializer.deserialize(type, node))
        } catch (ex: SerializationException) {
            ex.initPath { node.path() }
            EsuCore.instance.err("Could not deserialize ${node.raw()} into $type at ${node.path()}: ${ex.rawMessage()}")
            return Optional.empty<Any>()
        }
    }

    override fun serialize(type: Type, obj: Optional<*>?, node: ConfigurationNode) {
        if (type !is ParameterizedType) {
            throw SerializationException(type, "Raw types are not supported for optional")
        }
        if (type.actualTypeArguments.size != 1) {
            throw SerializationException(type, "Optional expected a type argument!")
        }
        if (obj == null || obj.isEmpty) {
            node.set(DISABLED)
        } else {
            val value = type.actualTypeArguments[0]
            val serializer = node.options().serializers()[value] ?: throw SerializationException(
                type, "No type serializer available for optional type $value"
            )
            @Suppress("UNCHECKED_CAST")
            (serializer as TypeSerializer<Any>).serialize(value, obj.get(), node)
        }
    }

}