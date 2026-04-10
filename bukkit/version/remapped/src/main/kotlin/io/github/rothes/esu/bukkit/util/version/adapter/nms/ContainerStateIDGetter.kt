package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.server.level.ServerPlayer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

interface ContainerStateIDGetter {

    fun getContainerStateId(player: Player): Int {
        return getContainerStateId((player as CraftPlayer).handle)
    }

    fun getContainerStateId(player: ServerPlayer): Int

    operator fun get(player: Player): Int = getContainerStateId(player)
    operator fun get(player: ServerPlayer): Int = getContainerStateId(player)

}