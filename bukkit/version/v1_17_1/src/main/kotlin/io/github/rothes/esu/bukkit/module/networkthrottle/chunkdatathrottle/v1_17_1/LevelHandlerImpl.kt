package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_17_1

import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.LevelHandler
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

class LevelHandlerImpl: LevelHandler {

    override fun level(player: ServerPlayer): ServerLevel {
        return player.getLevel()
    }

}