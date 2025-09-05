package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.module.NetworkThrottleModule
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.version.Version
import org.incendo.cloud.annotations.Command

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

        NetworkThrottleModule.registerCommands(object {
            @Command("esu networkThrottle chunkDataThrottle stats")
            @ShortPerm("chunkDataThrottle")
            fun chunkDataThrottleView(sender: User) {
                val (minimalChunks, resentChunks, resentBlocks) = counter
                sender.message("minimalChunks: $minimalChunks ; resentChunks: $resentChunks ; resentBlocks: $resentBlocks")
            }
        })
    }

    fun onDisable() {
        if (ServerCompatibility.serverVersion < Version.fromString("1.18"))
            return
        versioned.disable()
    }

}