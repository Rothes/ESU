package io.github.rothes.esu.core.util

import sun.misc.Unsafe
import java.lang.reflect.Field

object UnsafeUtils {

    val unsafe: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").also {
        it.isAccessible = true
    }.get(null) as Unsafe

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

    @Suppress("DEPRECATION")
    private val Field.objOffset
        get() = unsafe.objectFieldOffset(this)
    @Suppress("DEPRECATION")
    private val Field.staticOffset
        get() = unsafe.staticFieldOffset(this)
    @Suppress("DEPRECATION")
    private val Field.staticBase
        get() = unsafe.staticFieldBase(this)

}