package io.github.rothes.esu.bukkit.util.artifact.injector

import io.github.rothes.esu.bukkit.plugin
import java.net.URL
import java.net.URLClassLoader

object ReflectURLInjector: URLInjector {

    override fun addURL(url: URL) {
        val method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java).also {
            it.isAccessible = true
        }
        method.invoke(plugin.javaClass.classLoader, url)
    }
}