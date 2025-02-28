package io.github.rothes.esu.core.configuration.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.util.function.Predicate

object RegexSerializer: ScalarSerializer<Regex>(Regex::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Regex {
        return obj.toString().toRegex()
    }

    override fun serialize(regex: Regex, typeSupported: Predicate<Class<*>>): String {
        return regex.pattern
    }

}
