package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import org.incendo.cloud.caption.Caption
import org.incendo.cloud.caption.CaptionProvider
import org.incendo.cloud.caption.ConstantCaptionProvider
import org.incendo.cloud.caption.DelegatingCaptionProvider
import java.lang.reflect.Type
import java.util.*
import java.util.function.Predicate

object CaptionSerializer: ScalarSerializer<Caption>(Caption::class.java) {

    private val field = EsuCore.instance.commandManager.captionRegistry()::class.java.getDeclaredField("providers").also {
        it.isAccessible = true
    }

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Caption {
        val key = obj.toString()
        // Find the instance from command manager
        val captionRegistry = EsuCore.instance.commandManager.captionRegistry()
        @Suppress("UNCHECKED_CAST")
        val providers = field[captionRegistry] as LinkedList<CaptionProvider<*>>
        providers.forEach {
            ((it as? DelegatingCaptionProvider<*>)?.delegate() as? ConstantCaptionProvider)?.let { provider ->
                provider.captions().keys
                    .find { caption ->
                        caption.key() == key
                    }?.let { caption ->
                        return caption
                    }
            }
        }
        // Create one if not exists
        return Caption.of(key)
    }

    override fun serialize(caption: Caption, typeSupported: Predicate<Class<*>>): String {
        return caption.key()
    }

}
