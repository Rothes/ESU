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

package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.configurate.util.EnumLookup
import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import java.lang.reflect.Type
import java.util.function.Predicate

object EnumValueSerializer: ScalarSerializer<Enum<*>>(TypeToken.get(Enum::class.java)) {

    override fun deserialize(type: Type, obj: Any): Enum<*>? {
        val string = obj.toString()
        val typeClass = GenericTypeReflector.erase(type).asSubclass(Enum::class.java)
        return EnumLookup.lookupEnum(typeClass, string)
            ?: EnumLookup.lookupEnum(typeClass, string.replace("-", "_"))
            ?: let {
                EsuCore.instance.warn("Invalid enum constant provided '$string'. Using null.")
                null
            }
    }

    public override fun serialize(item: Enum<*>, typeSupported: Predicate<Class<*>>): Any {
        return item.name.lowercase()
    }

}