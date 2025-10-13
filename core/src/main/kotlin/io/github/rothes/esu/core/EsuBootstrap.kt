package io.github.rothes.esu.core

import java.nio.file.Path

interface EsuBootstrap : EsuLogger {

    fun baseConfigPath(): Path

    companion object {
        private var instanceInternal: EsuBootstrap? = null

        val instance: EsuBootstrap
            get() = instanceInternal ?: error("EsuBootstrap instance is not set")

        fun setInstance(set: EsuBootstrap) {
            check(instanceInternal == null) { "EsuBootstrap instance already set!" }
            instanceInternal = set
        }
    }
}
