/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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