package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.lib.org.spongepowered.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.time.LocalDate
import java.util.function.Predicate

object LocalDateSerializer: ScalarSerializer<LocalDate>(LocalDate::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): LocalDate {
        val string = obj.toString()
        return LocalDate.parse(string)
    }

    override fun serialize(localDate: LocalDate, typeSupported: Predicate<Class<*>>): String {
        return localDate.toString()
    }

}
