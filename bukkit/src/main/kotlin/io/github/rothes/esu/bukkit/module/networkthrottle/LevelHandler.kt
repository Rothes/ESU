package io.github.rothes.esu.bukkit.module.networkthrottle

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

interface LevelHandler {

    fun level(player: ServerPlayer): ServerLevel

}