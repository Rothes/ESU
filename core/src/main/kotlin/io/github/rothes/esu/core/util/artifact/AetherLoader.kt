package io.github.rothes.esu.core.util.artifact

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.util.FileHashes.Companion.sha1
import io.github.rothes.esu.core.util.artifact.injector.UnsafeURLInjector
import java.io.IOException
import java.net.URI

object AetherLoader {

    init {
        try {
            Class.forName("org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory")
        } catch (_: ClassNotFoundException) {
            // Spigot 1.16.5 and older
            val resolve = EsuCore.instance.baseConfigPath().resolve(".cache/aether-library.jar").toFile()
            if (!resolve.exists() || resolve.sha1 != "f2bbafed1dd38ffdbaac1daf17ca706efbec74ef") {
                fun downloadAetherLib(domain: String) {
                    val url = URI.create("https://$domain/Rothes/ESU/raw/refs/heads/raw/aether-library.jar").toURL()
                    EsuCore.instance.info("Downloading $url to $resolve")
                    resolve.parentFile.mkdirs()
                    resolve.createNewFile()
                    url.openStream().use { stream ->
                        resolve.outputStream().use { outputStream ->
                            stream.copyTo(outputStream)
                        }
                    }
                }
                try {
                    downloadAetherLib("ghfast.top/https://github.com")
                } catch (_: IOException) {
                    EsuCore.instance.info("Connection error, fallback to another link")
                    downloadAetherLib("github.com")
                }
            }
            UnsafeURLInjector.addURL(resolve.toURI().toURL())
        }
    }

}