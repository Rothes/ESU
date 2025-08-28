package io.github.rothes.esu.core.util

import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGet
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import sun.misc.Unsafe
import java.lang.invoke.MethodHandle
import java.lang.reflect.Field

object UnsafeUtils {

    val unsafe: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").also {
        it.isAccessible = true
    }.get(null) as Unsafe

    private val internalUnsafe = unsafe.javaClass.getDeclaredField("theInternalUnsafe").accessibleGet(null)
    private val internalOffset: MethodHandle

    init {
        val internalOffsetMethod = internalUnsafe.javaClass.getDeclaredMethod("objectFieldOffset", Field::class.java)
        val bool = unsafe.getBoolean(internalOffsetMethod, 12)
        unsafe.putBoolean(internalOffsetMethod, 12, true) // Make it accessible
        internalOffset = internalOffsetMethod.handle(pType = Any::class.java) // This checks for accessible when we get it
        unsafe.putBoolean(internalOffsetMethod, 12, bool) // Set accessible back, we no longer need the hack
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

    val Field.usObjGetter
        get() = UnsafeObjGetter(this)
    val Field.usObjGetterNullable
        get() = UnsafeObjGetterNullable(this)

    val Field.usLongGetter
        get() = UnsafeLongGetter(this)
    val Field.usLongSetter
        get() = UnsafeLongSetter(this)

    @Suppress("DEPRECATION")
    private val Field.objOffset
        get() = try {
            unsafe.objectFieldOffset(this)
        } catch (_: UnsupportedOperationException) {
            internalOffset.invokeExact(internalUnsafe, this) as Long
        }
    @Suppress("DEPRECATION")
    private val Field.staticOffset
        get() = unsafe.staticFieldOffset(this)
    @Suppress("DEPRECATION")
    private val Field.staticBase
        get() = unsafe.staticFieldBase(this)

    class UnsafeObjGetter(val field: Field) {
        private val offset = field.objOffset

        // No Intrinsics.checkNotNull, should be faster
        operator fun get(obj: Any): Any = unsafe.getObject(obj, offset)
    }

    class UnsafeObjGetterNullable(val field: Field) {
        private val offset = field.objOffset

        operator fun get(obj: Any): Any? = unsafe.getObject(obj, offset)
    }

    class UnsafeLongGetter(val field: Field) {
        private val offset = field.objOffset

        operator fun get(obj: Any): Long = unsafe.getLong(obj, offset)
    }
    class UnsafeLongSetter(val field: Field) {
        private val offset = field.objOffset

        operator fun set(obj: Any, value: Long): Long = unsafe.getAndSetLong(obj, offset, value)
    }

}