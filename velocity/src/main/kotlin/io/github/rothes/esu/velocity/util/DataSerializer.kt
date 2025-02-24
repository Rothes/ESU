package io.github.rothes.esu.velocity.util

import com.google.gson.GsonBuilder
import kotlin.jvm.java

object DataSerializer {

    val GSON = GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().create()!!

    inline fun <reified T> serialize(data: T): ByteArray {
        return GSON.toJson(data, T::class.java).toByteArray(Charsets.UTF_8)
    }

    inline fun <reified T> deserialize(bytes: ByteArray): T {
        return GSON.fromJson(bytes.toString(Charsets.UTF_8), T::class.java)
    }

    fun Any.encode(): ByteArray = serialize(this)
    inline fun <reified T> ByteArray.decode(): T = deserialize(this)

}