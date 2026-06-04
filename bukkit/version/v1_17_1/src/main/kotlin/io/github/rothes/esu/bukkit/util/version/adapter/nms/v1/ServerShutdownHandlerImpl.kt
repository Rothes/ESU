package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ServerShutdownHandler
import net.minecraft.server.MinecraftServer

object ServerShutdownHandlerImpl : ServerShutdownHandler {

    override fun isRunning(): Boolean {
        return MinecraftServer.getServer().isRunning
    }

}