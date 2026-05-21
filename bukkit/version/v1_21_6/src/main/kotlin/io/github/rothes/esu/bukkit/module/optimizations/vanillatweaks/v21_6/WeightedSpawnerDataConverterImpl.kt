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

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.v21_6

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.BiomeSpawnersFeature
import net.minecraft.util.random.Weighted
import net.minecraft.util.random.WeightedList
import net.minecraft.world.level.biome.MobSpawnSettings

object WeightedSpawnerDataConverterImpl: BiomeSpawnersFeature.WeightedSpawnerDataConverter {

    override fun fromWeightedList(weightedList: Any): List<BiomeSpawnersFeature.BiomeSettings.WeightedSpawnerData> {
        @Suppress("UNCHECKED_CAST")
        weightedList as WeightedList<MobSpawnSettings.SpawnerData>
        return weightedList.unwrap().map {
            BiomeSpawnersFeature.BiomeSettings.WeightedSpawnerData(
                it.value.type, it.value.minCount, it.value.maxCount, it.weight
            )
        }
    }

    override fun toWeightedList(list: List<BiomeSpawnersFeature.BiomeSettings.WeightedSpawnerData>): Any {
        return WeightedList.of(
            list.map {
                Weighted(MobSpawnSettings.SpawnerData(it.entityType, it.minCount, it.maxCount), it.weight)
            }
        )
    }
}