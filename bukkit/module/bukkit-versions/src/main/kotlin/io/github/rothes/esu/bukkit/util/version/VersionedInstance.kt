/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.util.version

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.core.util.artifact.MavenResolver
import io.github.rothes.esu.core.util.version.Version
import io.github.rothes.esu.core.util.version.toVersion
import java.io.File
import java.lang.reflect.Modifier
import java.net.URL
import java.util.jar.JarFile

object VersionedInstance {

    private val versions = mutableListOf<URL>()
    private var classes = mutableListOf<String>()

    @JvmStatic
    fun <T> getVersioned(target: Class<T>, type: String? = null, version: Version = ServerInfo.mcVersion): T {
        return try {
            if (!target.isInterface && !Modifier.isAbstract(target.modifiers))
                error("${target.canonicalName} is not an interface or abstract class.")

            val prefix = target.packageName + ".v"
            val find = classes
                .filter {
                    it.startsWith(prefix)
                            && it.substringAfterLast('.').substringAfterLast('$').startsWith(target.simpleName)
                            && (type == null || it.endsWith(type))
                }
                .mapNotNull {
                    val str = it.substring(prefix.length).substringBefore('.')
                    val split = str.split("__")

                    if (split.size == 2) {
                        when (split[1]) {
                            "paper" -> if (!ServerInfo.isPaper) return@mapNotNull null
                        }
                    }
                    it to split[0].replace('_', '.').toVersion()
                }
                .sortedWith(Comparator { a, b -> compareValuesBy(b, a, { it.second }, { it.first.length }) })
                .firstOrNull { version >= it.second }

            if (find == null)
                error("${target.canonicalName} is not implemented for version $version, type $type")

            val clazz = Class.forName(find.first)
            if (!target.isAssignableFrom(clazz))
                error("Found ${clazz.name}, but it is not an instance of ${target.canonicalName}")

            @Suppress("UNCHECKED_CAST")
            (clazz.kotlin.objectInstance ?: clazz.getConstructor().newInstance()) as T
        } catch (e: Exception) {
            throw IllegalStateException("Cannot get versioned instance of ${target.canonicalName} for version $version, type $type", e)
        }
    }

    @JvmStatic
    inline fun <reified T> versioned(type: String? = null, version: Version = ServerInfo.mcVersion): T {
        val target = T::class.java
        return getVersioned(target, type, version)
    }

    @JvmStatic
    fun <T> versioned(target: Class<T>, type: String? = null, version: Version = ServerInfo.mcVersion): T {
        return getVersioned(target, type, version)
    }


    fun loadVersionJar(file: File) {
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