package io.github.rothes.esu.core.util.artifact.injector

import io.github.rothes.esu.core.EsuCore
import sun.misc.Unsafe
import java.lang.reflect.Field
import java.net.URL
import java.net.URLClassLoader

object UnsafeURLInjector: URLInjector {

    private val unsafe: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").also {
        it.isAccessible = true
    }.get(null) as Unsafe
    private val ucp: Any = URLClassLoader::class.java.getDeclaredField("ucp").unsafe(EsuCore.instance.javaClass.classLoader)
    private val path: MutableCollection<URL> = ucp.javaClass.getDeclaredField("path").unsafe(ucp)
    private val unopenedUrls: MutableCollection<URL> = ucp.javaClass.getDeclaredField("unopenedUrls").unsafe(ucp)

    override fun addURL(url: URL) {
        path.add(url)
        unopenedUrls.add(url)
    }

    private fun <T> Field.unsafe(obj: Any): T {
        val offset = unsafe.objectFieldOffset(this)
        @Suppress("UNCHECKED_CAST")
        return unsafe.getObject(obj, offset) as T
    }
}