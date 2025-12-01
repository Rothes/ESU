package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.world.level.block.state.BlockState

interface BlockOccludeTester {

    fun isFullOcclude(blockState: BlockState): Boolean

}