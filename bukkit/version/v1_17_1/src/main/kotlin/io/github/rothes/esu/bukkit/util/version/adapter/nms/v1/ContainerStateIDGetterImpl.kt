package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ContainerStateIDGetter
import net.minecraft.server.level.ServerPlayer

object ContainerStateIDGetterImpl: ContainerStateIDGetter {

    override fun getContainerStateId(player: ServerPlayer): Int {
        return player.inventoryMenu.stateId
    }

}