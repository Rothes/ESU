/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.configuration

import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.leangen.geantyref.TypeToken
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import java.lang.reflect.Type
import java.util.function.Predicate

class RegistryValueSerializer<T: Any>(
    val accessHandler: NmsRegistryAccessHandler,
    registryKey: ResourceKey<out Registry<T>>,
    type: TypeToken<T>,
): ScalarSerializer<T>(type) {

    companion object {
        private val KEY_HANDLER by Versioned(ResourceKeyHandler::class.java)
    }

    constructor(accessHandler: NmsRegistryAccessHandler, registryKey: ResourceKey<out Registry<T>>, clazz: Class<T>): this(accessHandler, registryKey, TypeToken.get(clazz))

    val registry = accessHandler.getRegistryOrThrow(registryKey)

    override fun deserialize(type: Type?, obj: Any?): T? {
        val key = try {
            KEY_HANDLER.parseResourceKey(registry, obj.toString().lowercase())
        } catch (e: ResourceKeyHandler.BadIdentifierException) {
            e.printStackTrace()
            return null
        }
        return accessHandler.getNullable(registry, key) ?: let {
            IllegalArgumentException("Key $obj is not in the registry $registry, ignored.").printStackTrace()
            null
        }
    }

    override fun serialize(item: T, typeSupported: Predicate<Class<*>?>?): Any {
        val key = accessHandler.getResourceKey(registry, item)
        return KEY_HANDLER.getResourceKeyString(key)
    }

}