/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.v1

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.BiomeSpawnersFeature
import net.minecraft.util.random.WeightedRandomList
import net.minecraft.world.level.biome.MobSpawnSettings

object WeightedSpawnerDataConverterImpl: BiomeSpawnersFeature.WeightedSpawnerDataConverter {

    override fun fromWeightedList(weightedList: Any): List<BiomeSpawnersFeature.BiomeSettings.WeightedSpawnerData> {
        @Suppress("UNCHECKED_CAST")
        weightedList as WeightedRandomList<MobSpawnSettings.SpawnerData>
        return weightedList.unwrap().map {
            BiomeSpawnersFeature.BiomeSettings.WeightedSpawnerData(
                it.type, it.minCount, it.minCount, it.weight.asInt()
            )
        }
    }

    override fun toWeightedList(list: List<BiomeSpawnersFeature.BiomeSettings.WeightedSpawnerData>): Any {
        return WeightedRandomList.create(
            list.map {
                MobSpawnSettings.SpawnerData(
                    it.entityType, it.weight, it.minCount, it.maxCount
                )
            }
        )
    }
}