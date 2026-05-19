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

package io.github.rothes.esu.core.util

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Field
import java.lang.reflect.InaccessibleObjectException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.function.Predicate

object ReflectionUtils {

    private val LOOKUP = MethodHandles.lookup()

    fun getter(field: Field): MethodHandle {
        field.setAccessible(true)
        return LOOKUP.unreflectGetter(field)
    }

    fun method(method: Method, rType: Class<*>, pType: Class<*>, argTypes: Array<Class<*>>): MethodHandle {
        try {
            method.setAccessible(true)
        } catch (_: InaccessibleObjectException) {
        }
        // Not using buildList cuz stdlib is not loaded on bootstrap stage.
        val pTypes = mutableListOf<Class<*>>().apply {
            if (method.modifiers and Modifier.STATIC == 0) add(pType)
            argTypes.forEach { add(it) }
        }
        return LOOKUP.unreflect(method).asType(MethodType.methodType(rType, pTypes))
    }

    fun getter(field: Field, rType: Class<*> = field.type, pType: Class<*> = field.declaringClass): MethodHandle {
        return getter(field).asType(MethodType.methodType(rType, pType))
    }

    val Field.getter
        get() = getter()

    @JvmName("getterKt")
    fun Field.getter(rType: Class<*> = type, pType: Class<*> = declaringClass): MethodHandle {
        return ReflectionUtils.getter(this, rType, pType)
    }

    val Method.handle
        get() = handle()

    @JvmName("handleKt")
    fun Method.handle(rType: Class<*> = returnType, pType: Class<*> = declaringClass, argTypes: Array<Class<*>> = parameterTypes): MethodHandle {
        return method(this, rType, pType, argTypes)
    }

    fun Class<*>.findSuperClass(predicate: (Class<*>) -> Boolean): Class<*>? {
        val clazz = this.superclass ?: return null
        if (predicate(clazz)) return clazz
        return clazz.findSuperClass(predicate)
    }

    fun Class<*>.findSuperClassOrSelf(predicate: (Class<*>) -> Boolean): Class<*>? {
        if (predicate(this)) return this
        return findSuperClass(predicate)
    }

    fun Class<*>.findSuperClass(predicate: Predicate<Class<*>>): Class<*>? {
        val clazz = this.superclass ?: return null
        if (predicate.test(clazz)) return clazz
        return clazz.findSuperClass(predicate)
    }

    fun Class<*>.findSuperClassOrSelf(predicate: Predicate<Class<*>>): Class<*>? {
        if (predicate.test(this)) return this
        return findSuperClass(predicate)
    }

    // Easy functions
    fun Field.accessibleGet(any: Any?): Any {
        return accessibleGetT(any)
    }
    fun <T> Field.accessibleGetT(any: Any?): T {
        isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return this.get(any) as T
    }

    operator fun MethodHandle.get(any: Any?): Any {
        return invoke(any)
    }

    operator fun MethodHandle.get(vararg any: Any?): Any {
        return invokeWithArguments(any)
    }

}