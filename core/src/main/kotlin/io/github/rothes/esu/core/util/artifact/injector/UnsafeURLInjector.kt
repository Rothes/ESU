package io.github.rothes.esu.core.util.artifact.injector

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.UnsafeUtils.usGet
import java.net.URL
import java.net.URLClassLoader

object UnsafeURLInjector: URLInjector {

    private val ucp: Any = URLClassLoader::class.java.getDeclaredField("ucp").usGet(EsuBootstrap.instance.javaClass.classLoader)
    private val path: MutableCollection<URL> = ucp.javaClass.getDeclaredField("path").usGet(ucp)
    private val unopenedUrls: MutableCollection<URL> = ucp.javaClass.getDeclaredField("unopenedUrls").usGet(ucp)

    override fun addURL(url: URL) {
        path.add(url)
        unopenedUrls.add(url)
    }

}