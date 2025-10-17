package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.time.Duration
import java.util.function.Predicate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

object JavaDurationSerializer: ScalarSerializer<Duration>(Duration::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Duration {
        when (obj) {
            is Long -> return Duration.ofMillis(obj)
            is Int -> return Duration.ofMillis(obj.toLong())
        }
        val string = obj.toString()
        return kotlin.time.Duration.parse(string).toJavaDuration()
    }

    override fun serialize(duration: Duration, typeSupported: Predicate<Class<*>>): String {
        return duration.toKotlinDuration().toString()
    }

}
