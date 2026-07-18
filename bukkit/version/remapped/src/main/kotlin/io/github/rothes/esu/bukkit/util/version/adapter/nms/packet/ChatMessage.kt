package io.github.rothes.esu.bukkit.util.version.adapter.nms.packet

import net.minecraft.network.chat.ChatType
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component as MinecraftComponent

interface ChatMessage {

    fun sendChatMessage(player: ServerPlayer, message: MinecraftComponent, chatType: ChatType.Bound)

}