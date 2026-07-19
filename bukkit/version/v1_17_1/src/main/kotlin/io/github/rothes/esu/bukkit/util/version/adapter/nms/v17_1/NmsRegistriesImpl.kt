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

import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import net.minecraft.core.Registry
import net.minecraft.network.chat.ChatType
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

object NmsRegistriesImpl: NmsRegistries {

    override val biome: ResourceKey<Registry<Biome>> = Registry.BIOME_REGISTRY
    override val block: ResourceKey<Registry<Block>> = Registry.BLOCK_REGISTRY
    override val blockEntityType: ResourceKey<Registry<BlockEntityType<*>>> = Registry.BLOCK_ENTITY_TYPE_REGISTRY
    override val chatType: ResourceKey<Registry<ChatType>>
        get() = throw IllegalStateException("Not supported server version") // TODO: Confirm which version added
    override val entityType: ResourceKey<Registry<EntityType<*>>> = Registry.ENTITY_TYPE_REGISTRY

}