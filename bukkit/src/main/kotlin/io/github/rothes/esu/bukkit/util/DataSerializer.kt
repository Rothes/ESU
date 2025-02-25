package io.github.rothes.esu.bukkit.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.Location
import java.lang.reflect.Type

object DataSerializer {

    val GSON = GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization()
        .registerTypeAdapter(Location::class.java, BukkitLocationAdapter()).create()!!

    inline fun <reified T> serialize(data: T): ByteArray {
        return GSON.toJson(data, T::class.java).toByteArray(Charsets.UTF_8)
    }

    inline fun <reified T> deserialize(bytes: ByteArray, typeToken: TypeToken<T> = TypeToken.get(T::class.java)): T {
        return GSON.fromJson(bytes.toString(Charsets.UTF_8), typeToken.type)
    }

    fun Any.encode(): ByteArray = serialize(this)
    inline fun <reified T> ByteArray.decode(): T = deserialize(this)
    inline fun <reified T> ByteArray.decode(typeToken: TypeToken<T>): T = deserialize(this, typeToken)

    private class BukkitLocationAdapter : JsonSerializer<Location>, JsonDeserializer<Location> {

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
}