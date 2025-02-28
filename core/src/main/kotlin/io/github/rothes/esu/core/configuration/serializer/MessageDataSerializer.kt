package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.core.configuration.data.MessageData
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.util.function.Predicate

object MessageDataSerializer: ScalarSerializer<MessageData>(MessageData::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): MessageData {
        return MessageData.parse(obj.toString())
    }

    override fun serialize(data: MessageData, typeSupported: Predicate<Class<*>>): String {
        return data.string
    }

}