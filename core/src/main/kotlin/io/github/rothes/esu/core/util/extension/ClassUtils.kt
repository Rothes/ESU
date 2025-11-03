package io.github.rothes.esu.core.util.extension

import java.util.jar.JarFile

object ClassUtils {

    val Class<*>.jarFilePath: String
        get() = protectionDomain.codeSource.location.toURI().path
    val Class<*>.jarFile: JarFile
        get() = JarFile(jarFilePath)

}