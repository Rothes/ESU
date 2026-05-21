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

package io.github.rothes.esu.bukkit.util

import com.google.gson.*
import io.github.rothes.esu.bukkit.config.serializer.AttributeSerializer
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.util.DataSerializer
import io.github.rothes.esu.lib.nbtapi.NBT
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type

object BukkitDataSerializer {

    init {
        DataSerializer.registerTypeAdapter(
            Location::class.java to BukkitLocationAdapter,
            ItemStack::class.java to ItemStackAdapter
        )
        ConfigLoader.registerSerializer(AttributeSerializer)
    }

    private object BukkitLocationAdapter : JsonSerializer<Location>, JsonDeserializer<Location> {

        override fun serialize(location: Location, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty("world", location.world?.name)
                addProperty("x"    , location.x)
                addProperty("y"    , location.y)
                addProperty("z"    , location.z)
                addProperty("yaw"  , location.yaw)
                addProperty("pitch", location.pitch)
            }
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Location {
            val obj = json.asJsonObject
            return Location(obj["world"]?.asString?.let { Bukkit.getWorld(it) },
                obj["x"].asDouble, obj["y"].asDouble, obj["z"].asDouble, obj["yaw"].asFloat, obj["pitch"].asFloat)
        }

    }

    private object ItemStackAdapter : JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

        override fun serialize(item: ItemStack, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(NBT.itemStackToNBT(item).toString())
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemStack {
            val nbt = json.asJsonPrimitive.asString
            return NBT.itemStackFromNBT(NBT.parseNBT(nbt)) ?: error("Could not deserialize $nbt")
        }

    }

}