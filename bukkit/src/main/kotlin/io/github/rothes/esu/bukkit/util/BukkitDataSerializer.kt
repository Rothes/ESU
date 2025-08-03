package io.github.rothes.esu.bukkit.util

import com.google.gson.*
import de.tr7zw.changeme.nbtapi.NBT
import io.github.rothes.esu.core.util.DataSerializer
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