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