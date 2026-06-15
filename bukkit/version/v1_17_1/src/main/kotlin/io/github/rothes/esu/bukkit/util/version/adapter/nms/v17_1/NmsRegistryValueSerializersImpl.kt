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

import io.github.rothes.esu.bukkit.configuration.RegistryValueSerializer
import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryValueSerializers
import io.leangen.geantyref.TypeToken
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

object NmsRegistryValueSerializersImpl: NmsRegistryValueSerializers {

    private val nmsRegistryAccessHandler = versioned<NmsRegistryAccessHandler>()
    private val nmsRegistries = versioned<NmsRegistries>()

    override val biome = RegistryValueSerializer(nmsRegistryAccessHandler, nmsRegistries.biome, object : TypeToken<Biome>() {})
    override val block = RegistryValueSerializer(nmsRegistryAccessHandler, nmsRegistries.block, object : TypeToken<Block>() {})
    override val blockEntityType = RegistryValueSerializer(nmsRegistryAccessHandler, nmsRegistries.blockEntityType, object : TypeToken<BlockEntityType<*>>() {})
    override val entityType = RegistryValueSerializer(nmsRegistryAccessHandler, nmsRegistries.entityType, object : TypeToken<EntityType<*>>() {})

}