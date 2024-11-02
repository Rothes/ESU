package io.github.rothes.esu.core.configuration.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.util.function.Predicate
import kotlin.time.Duration

object DurationSerializer: ScalarSerializer<Duration>(Duration::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Duration {
        val string = obj.toString()
        return Duration.parse(string)
    }

    override fun serialize(duration: Duration, typeSupported: Predicate<Class<*>>): String {
        return duration.toString()
    }

}
