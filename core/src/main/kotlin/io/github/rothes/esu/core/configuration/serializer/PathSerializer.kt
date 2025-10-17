package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Predicate
import java.util.regex.Matcher

object PathSerializer: ScalarSerializer<Path>(Path::class.java) {

    override fun deserialize(type: Type, obj: Any): Path {
        val string = obj.toString()
        return Paths.get(string.replaceFirst("^~/?".toRegex(), Matcher.quoteReplacement(System.getProperty("user.home") + '/')))
    }

    override fun serialize(path: Path, typeSupported: Predicate<Class<*>>): String {
        return path.toAbsolutePath().toString()
    }

}
