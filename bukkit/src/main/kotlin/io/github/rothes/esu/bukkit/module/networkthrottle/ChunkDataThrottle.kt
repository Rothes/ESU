package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.core.util.version.Version

object ChunkDataThrottle {

    val versioned by Versioned(ChunkDataThrottleHandler::class.java)
    val counter
        get() = versioned.counter

    fun onReload() {
        versioned.reload()
    }

    fun onEnable() {
        if (ServerCompatibility.serverVersion < Version.fromString("1.18")) {
            if (config.chunkDataThrottle.enabled)
                plugin.err("[ChunkDataThrottle] This feature requires Minecraft 1.18+")
            return
        }
        versioned.enable()
    }

    fun onDisable() {
        if (ServerCompatibility.serverVersion < Version.fromString("1.18"))
            return
        versioned.disable()
    }

}