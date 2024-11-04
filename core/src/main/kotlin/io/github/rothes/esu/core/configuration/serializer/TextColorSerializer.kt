package io.github.rothes.esu.core.configuration.serializer

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.util.function.Predicate

object TextColorSerializer: ScalarSerializer<TextColor>(TextColor::class.java) {

    private val aliases = mapOf(Pair("dark_grey", NamedTextColor.DARK_GRAY), Pair("grey", NamedTextColor.GRAY))

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): TextColor {
        val string = obj.toString()
        return (
                if (string.startsWith("#")) TextColor.fromHexString(string)
                else NamedTextColor.NAMES.value(string) ?: aliases[string]
                ) ?: throw SerializationException("Unable to parse a color from '$string'. Please use named colors or hex (#RRGGBB) colors.")
    }

    override fun serialize(textColor: TextColor, typeSupported: Predicate<Class<*>>): String {
        return textColor.toString()
    }

}
