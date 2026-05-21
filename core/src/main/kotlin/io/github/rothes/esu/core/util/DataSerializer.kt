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

package io.github.rothes.esu.core.util

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
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
        return GsonBuilder()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .addSerializationExclusionStrategy(object : ExclusionStrategy {
                override fun shouldSkipField(f: FieldAttributes): Boolean {
                    val expose = f.getAnnotation(Expose::class.java) ?: return false
                    return !expose.serialize
                }

                override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                    return false
                }
            })
            .apply {
                for ((type, typeAdapter) in typeAdapters) {
                    registerTypeAdapter(type, typeAdapter)
                }
            }.create()
    }

}