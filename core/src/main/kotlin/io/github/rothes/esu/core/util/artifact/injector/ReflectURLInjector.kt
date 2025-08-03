package io.github.rothes.esu.core.util.artifact.injector

import io.github.rothes.esu.core.EsuCore
import java.net.URL
import java.net.URLClassLoader

object ReflectURLInjector: URLInjector {

    override fun addURL(url: URL) {
        val method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java).also {
            it.isAccessible = true
        }
        method.invoke(EsuCore.instance.javaClass.classLoader, url)
    }
}