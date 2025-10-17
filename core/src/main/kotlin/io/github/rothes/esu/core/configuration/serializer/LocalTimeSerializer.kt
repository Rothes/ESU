package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.time.LocalTime
import java.util.function.Predicate

object LocalTimeSerializer: ScalarSerializer<LocalTime>(LocalTime::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): LocalTime {
        val string = obj.toString()
        return LocalTime.parse(string)
    }

    override fun serialize(localTime: LocalTime, typeSupported: Predicate<Class<*>>): String {
        return localTime.toString()
    }

}
