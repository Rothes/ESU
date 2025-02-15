package io.github.rothes.esu.core.configuration.serializer

import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.util.EnumLookup
import java.lang.reflect.Type
import java.util.*
import java.util.function.Predicate
import kotlin.Any
import kotlin.Enum
import kotlin.Throws

object EnumValueSerializer: ScalarSerializer<Enum<*>>(TypeToken.get(Enum::class.java)) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Enum<*> {
        val string = obj.toString()
        val typeClass = GenericTypeReflector.erase(type).asSubclass(Enum::class.java)
        return EnumLookup.lookupEnum(typeClass, string)
            ?: EnumLookup.lookupEnum(typeClass, string.replace("-", "_"))
            ?: throw SerializationException("Invalid enum constant provided '$string'.")
    }

    public override fun serialize(item: Enum<*>, typeSupported: Predicate<Class<*>>): Any {
        return item.name.lowercase()
    }

}