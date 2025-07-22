package io.github.rothes.esu.bukkit.util.version

import com.google.common.reflect.ClassPath
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.util.version.Version
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


class Versioned<T, V>(
    target: Class<V>,
    type: String? = null,
    version: Version = plugin.serverVersion,
): ReadOnlyProperty<T, V> {

    val handle =
        try {
            if (!target.isInterface)
                error("${target.canonicalName} is not an interface.")

            val prefix = target.packageName + ".v"
            val find = ClassPath.from(target.classLoader)
                .allClasses
                .filter { it.packageName.startsWith(prefix) && (type == null || it.name.endsWith(type)) }
                .map {
                    it to Version.fromString(
                        it.packageName.substring(prefix.length).substringBefore('.').replace('_', '.')
                    )
                }
                .sortedByDescending { it.second }
                .firstOrNull { version >= it.second }

            if (find == null)
                error("${target.canonicalName} is not implemented for version $version, type $type")

            val clazz = find.first.load()
            if (!target.isAssignableFrom(clazz))
                error("Found ${clazz.canonicalName}, but it is not an instance of ${target.canonicalName}")

            @Suppress("UNCHECKED_CAST")
            clazz.getConstructor().newInstance() as V
        } catch (e: Exception) {
            throw IllegalStateException("Cannot get versioned instance of ${target.canonicalName} for version $version, type $type", e)
        }

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return handle
    }

}