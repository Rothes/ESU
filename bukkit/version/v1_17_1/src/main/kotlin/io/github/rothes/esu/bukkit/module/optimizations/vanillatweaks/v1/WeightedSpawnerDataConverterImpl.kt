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