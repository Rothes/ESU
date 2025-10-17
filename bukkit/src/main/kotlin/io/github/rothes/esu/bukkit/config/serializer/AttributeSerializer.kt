package io.github.rothes.esu.bukkit.config.serializer

import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter.Companion.key_
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import org.bukkit.attribute.Attribute
import java.lang.reflect.Type
import java.util.function.Predicate

object AttributeSerializer: ScalarSerializer<Attribute>(Attribute::class.java) {

    override fun deserialize(type: Type, obj: Any): Attribute {
        return AttributeAdapter.of(obj.toString()) ?: error("Unknown attribute $obj")
    }

    override fun serialize(data: Attribute, typeSupported: Predicate<Class<*>>): String {
        return data.key_
    }

}