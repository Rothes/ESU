package io.github.rothes.esu.core.util

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Field

object ReflectionUtils {

    private val LOOKUP = MethodHandles.lookup()

    fun getter(field: Field): MethodHandle {
        field.setAccessible(true)
        return LOOKUP.unreflectGetter(field)
    }

    fun getter(field: Field, rType: Class<*> = field.type, pType: Class<*> = field.declaringClass): MethodHandle {
        return getter(field).asType(MethodType.methodType(rType, pType))
    }

    val Field.getter
        get() = getter(this)

    @JvmName("getterKt")
    fun Field.getter(rType: Class<*> = type, pType: Class<*> = declaringClass): MethodHandle {
        return ReflectionUtils.getter(this, rType, pType)
    }

}