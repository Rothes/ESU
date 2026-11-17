package io.github.rothes.esu.core.util.artifact.injector

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.UnsafeUtils.handleOverride
import java.net.URL
import java.net.URLClassLoader

object UnsafeMethodURLInjector: URLInjector {

    private val method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java).handleOverride()

    override fun addURL(url: URL) {
        method.invoke(EsuBootstrap.instance.javaClass.classLoader, url)
    }

}