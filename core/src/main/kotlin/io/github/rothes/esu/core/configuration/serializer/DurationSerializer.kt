package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.lib.org.spongepowered.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.util.function.Predicate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object DurationSerializer: ScalarSerializer<Duration>(Duration::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Duration {
        when (obj) {
            is Long -> return obj.milliseconds
            is Int -> return obj.milliseconds
        }
        val string = obj.toString()
        return Duration.parse(string)
    }

    override fun serialize(duration: Duration, typeSupported: Predicate<Class<*>>): String {
        return duration.toString()
    }

}
