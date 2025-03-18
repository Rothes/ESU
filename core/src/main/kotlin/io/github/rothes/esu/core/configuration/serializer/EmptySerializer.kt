package io.github.rothes.esu.core.configuration.serializer

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class EmptySerializer<T>: TypeSerializer<T> {

    override fun deserialize(type: Type?, node: ConfigurationNode?): T? {
        return null
    }

    override fun serialize(type: Type?, obj: T?, node: ConfigurationNode?) {
    }

}