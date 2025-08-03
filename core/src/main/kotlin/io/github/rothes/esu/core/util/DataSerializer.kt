package io.github.rothes.esu.core.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object DataSerializer {

    private val typeAdapters = mutableListOf<Pair<Type, Any>>()

    var gson = createGson()
        private set

    fun registerTypeAdapter(vararg pairs: Pair<Type, Any>) {
        typeAdapters.addAll(pairs)
        gson = createGson()
    }

    inline fun <reified T> serializeObj(data: T): String {
        return gson.toJson(data, T::class.java)
    }

    inline fun <reified T> deserializeObj(str: String, typeToken: TypeToken<T> = TypeToken.get(T::class.java)): T {
        return gson.fromJson(str, typeToken.type)
    }

    fun Any.serialize(): String = serializeObj(this)
    inline fun <reified T> String.deserialize(): T = deserializeObj(this)
    inline fun <reified T> String.deserialize(typeToken: TypeToken<T>): T = deserializeObj(this, typeToken)

    fun Any.encode(): ByteArray = this.serialize().toByteArray(Charsets.UTF_8)
    inline fun <reified T> ByteArray.decode(): T = this.toString(Charsets.UTF_8).deserialize()
    inline fun <reified T> ByteArray.decode(typeToken: TypeToken<T>): T = this.toString(Charsets.UTF_8).deserialize(typeToken)

    private fun createGson(): Gson {
        return GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().apply {
            for ((type, typeAdapter) in typeAdapters) {
                registerTypeAdapter(type, typeAdapter)
            }
        }.create()
    }

}