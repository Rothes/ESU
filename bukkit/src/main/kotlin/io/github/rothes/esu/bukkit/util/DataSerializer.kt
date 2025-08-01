package io.github.rothes.esu.bukkit.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import de.tr7zw.changeme.nbtapi.NBT
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type

object DataSerializer {

    val GSON = GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization()
        .registerTypeAdapter(Location::class.java, BukkitLocationAdapter)
        .registerTypeAdapter(ItemStack::class.java, ItemStackAdapter)
        .create()!!

    inline fun <reified T> serializeObj(data: T): String {
        return GSON.toJson(data, T::class.java)
    }

    inline fun <reified T> deserializeObj(str: String, typeToken: TypeToken<T> = TypeToken.get(T::class.java)): T {
        return GSON.fromJson(str, typeToken.type)
    }

    fun Any.serialize(): String = serializeObj(this)
    inline fun <reified T> String.deserialize(): T = deserializeObj(this)
    inline fun <reified T> String.deserialize(typeToken: TypeToken<T>): T = deserializeObj(this, typeToken)

    fun Any.encode(): ByteArray = this.serialize().toByteArray(Charsets.UTF_8)
    inline fun <reified T> ByteArray.decode(): T = this.toString(Charsets.UTF_8).deserialize()
    inline fun <reified T> ByteArray.decode(typeToken: TypeToken<T>): T = this.toString(Charsets.UTF_8).deserialize(typeToken)

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