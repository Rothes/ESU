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