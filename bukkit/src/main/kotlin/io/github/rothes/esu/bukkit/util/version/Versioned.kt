package io.github.rothes.esu.bukkit.util.version

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.util.artifact.MavenResolver
import io.github.rothes.esu.core.util.version.Version
import java.io.File
import java.lang.reflect.Modifier
import java.net.URL
import java.util.jar.JarFile
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Versioned<T, V>(
    target: Class<V>,
    type: String? = null,
    version: Version = ServerCompatibility.serverVersion,
): ReadOnlyProperty<T, V> {

    val handle =
        try {
            if (!target.isInterface && !Modifier.isAbstract(target.modifiers))
                error("${target.canonicalName} is not an interface or abstract class.")

            val prefix = target.packageName + ".v"
            val find = classes
                .filter {
                    !it.contains('$') // Not subclass!
                            && it.startsWith(prefix)
                            && it.substringAfterLast('.').startsWith(target.simpleName)
                            && (type == null || it.endsWith(type))
                }
                .mapNotNull {
                    val str = it.substring(prefix.length).substringBefore('.')
                    val split = str.split("__")

                    if (split.size == 2) {
                        if (split[1] == "paper" && !ServerCompatibility.isPaper) {
                            return@mapNotNull null
                        }
                    }
                    it to Version.fromString(split[0].replace('_', '.'))
                }
                .sortedWith(Comparator { a, b -> compareValuesBy(b, a, { it.second }, { it.first.length }) })
                .firstOrNull { version >= it.second }

            if (find == null)
                error("${target.canonicalName} is not implemented for version $version, type $type")

            val clazz = Class.forName(find.first)
            if (!target.isAssignableFrom(clazz))
                error("Found ${clazz.canonicalName}, but it is not an instance of ${target.canonicalName}")

            @Suppress("UNCHECKED_CAST")
            (clazz.kotlin.objectInstance ?: clazz.getConstructor().newInstance()) as V
        } catch (e: Exception) {
            throw IllegalStateException("Cannot get versioned instance of ${target.canonicalName} for version $version, type $type", e)
        }

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return handle
    }

    companion object {
        private val versions = mutableListOf<URL>()
        private var classes = mutableListOf<String>()

        fun loadVersion(file: File) {
            val url = file.toURI().toURL()
            MavenResolver.loadUrl(url)
            versions.add(url)

            JarFile(file).use { jarFile ->
                val entries = jarFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".class")) {
                        classes.add(entry.name.dropLast(".class".length).replace('/', '.'))
                    }
                }
            }
        }

    }

}