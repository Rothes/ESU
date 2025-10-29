package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

interface LevelHandler {

    fun level(player: ServerPlayer): ServerLevel

}