package io.github.rothes.esu.bukkit.util.version.adapter.nms.v17_1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.BlockOccludeTester
import io.github.rothes.esu.core.util.UnsafeUtils.usBooleanAccessor
import io.github.rothes.esu.core.util.UnsafeUtils.usNullableObjAccessor
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

object BlockOccludeTesterImpl: BlockOccludeTester {

    private val canOcclude = BlockBehaviour.BlockStateBase::class.java.getDeclaredField("canOcclude").usBooleanAccessor
    private val bsCache = BlockBehaviour.BlockStateBase::class.java.getDeclaredField("cache").usNullableObjAccessor

    override fun isFullOcclude(blockState: BlockState): Boolean {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // No worries
        return canOcclude[blockState]
                && bsCache[blockState] != null
                && blockState.isCollisionShapeFullBlock(null, null)
    }

}