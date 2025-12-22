package io.github.rothes.esu.bukkit.util.version.adapter.nms.v17_1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

object NmsRegistriesImpl: NmsRegistries {

    override val biome: ResourceKey<Registry<Biome>> = Registry.BIOME_REGISTRY
    override val block: ResourceKey<Registry<Block>> = Registry.BLOCK_REGISTRY
    override val blockEntityType: ResourceKey<Registry<BlockEntityType<*>>> = Registry.BLOCK_ENTITY_TYPE_REGISTRY
    override val entityType: ResourceKey<Registry<EntityType<*>>> = Registry.ENTITY_TYPE_REGISTRY

}