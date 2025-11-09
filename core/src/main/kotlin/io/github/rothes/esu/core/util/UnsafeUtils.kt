package io.github.rothes.esu.core.util

import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGet
import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGetT
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import sun.misc.Unsafe
import java.lang.invoke.MethodHandle
import java.lang.management.ManagementFactory
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnsafeUtils {

    val unsafe: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").accessibleGetT(null)

    private val internalUnsafe = unsafe.javaClass.getDeclaredField("theInternalUnsafe").accessibleGet(null)
    private val internalOffset = internalUnsafe.javaClass.getDeclaredMethod("objectFieldOffset", Field::class.java).handleOverride(pType = Any::class.java)

    fun Method.handleOverride(rType: Class<*> = returnType, pType: Class<*> = declaringClass, argTypes: Array<Class<*>> = parameterTypes): MethodHandle {
        val newHeader = Runtime.version().version()[0] >= 24 && ManagementFactory.getRuntimeMXBean().inputArguments.contains("-XX:+UseCompactObjectHeaders")
        val overrideOffset = if (newHeader) 8L else 12L
        val bool = unsafe.getBoolean(this, overrideOffset)
        try {
            unsafe.putBoolean(this, overrideOffset, true) // Make it accessible
            return this.handle(rType = rType, pType = pType, argTypes = argTypes) // This checks for accessible when we get it
        } finally {
            unsafe.putBoolean(this, overrideOffset, bool) // Set accessible back, we no longer need the hack
        }
    }

    fun <T> Field.usGet(obj: Any?): T {
        val offset = obj?.let { objOffset } ?: staticOffset
        @Suppress("UNCHECKED_CAST")
        return unsafe.getObject(obj ?: staticBase, offset) as T
    }

    fun Field.usSet(obj: Any?, value: Int) {
        val offset = obj?.let { objOffset } ?: staticOffset
        unsafe.putInt(obj ?: staticBase, offset, value)
    }

    fun Field.usSet(obj: Any?, value: Any) {
        val offset = obj?.let { objOffset } ?: staticOffset
        unsafe.putObject(obj ?: staticBase, offset, value)
    }

    val Field.usObjAccessor
        get() = UnsafeObjAccessor(this)
    val Field.usNullableObjAccessor
        get() = UnsafeNullableObjAccessor(this)

    val Field.usBooleanAccessor
        get() = UnsafeBooleanAccessor(this)
    val Field.usIntAccessor
        get() = UnsafeIntAccessor(this)
    val Field.usLongAccessor
        get() = UnsafeLongAccessor(this)

    private val Field.objOffset
        get() = try {
            @Suppress("DEPRECATION")
            unsafe.objectFieldOffset(this)
        } catch (_: UnsupportedOperationException) {
            internalOffset.invokeExact(internalUnsafe, this) as Long
        }
    private val Field.staticOffset
        @Suppress("DEPRECATION")
        get() = unsafe.staticFieldOffset(this)
    private val Field.staticBase
        @Suppress("DEPRECATION")
        get() = unsafe.staticFieldBase(this)

    class UnsafeObjAccessor(field: Field): UnsafeFieldAccessor(field) {
        // No Kotlin Intrinsics.checkNotNull, should be faster
        operator fun get(obj: Any?): Any = unsafe.getObject(obj, offset)
        operator fun set(obj: Any?, value: Any?) = unsafe.putObject(obj, offset, value)
    }

    class UnsafeNullableObjAccessor(field: Field): UnsafeFieldAccessor(field) {
        operator fun get(obj: Any?): Any? = unsafe.getObject(obj, offset)
        operator fun set(obj: Any?, value: Any?) = unsafe.putObject(obj, offset, value)
    }

    class UnsafeBooleanAccessor(field: Field): UnsafeFieldAccessor(field) {
        operator fun get(obj: Any?): Boolean = unsafe.getBoolean(obj, offset)
        operator fun set(obj: Any?, value: Boolean) = unsafe.putBoolean(obj, offset, value)
    }

    class UnsafeIntAccessor(field: Field): UnsafeFieldAccessor(field) {
        operator fun get(obj: Any?): Int = unsafe.getInt(obj, offset)
        operator fun set(obj: Any?, value: Int) = unsafe.putInt(obj, offset, value)
    }

    class UnsafeLongAccessor(field: Field): UnsafeFieldAccessor(field) {
        operator fun get(obj: Any?): Long = unsafe.getLong(obj, offset)
        operator fun set(obj: Any?, value: Long) = unsafe.putLong(obj, offset, value)
    }

    abstract class UnsafeFieldAccessor(val field: Field) {
        protected val offset = if (Modifier.isStatic(field.modifiers)) field.staticOffset else field.objOffset
    }

}