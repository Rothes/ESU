package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_17_1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

object MCRegistriesImpl: MCRegistries {

    override val block: ResourceKey<Registry<Block>> = Registry.BLOCK_REGISTRY
    override val blockEntityType: ResourceKey<Registry<BlockEntityType<*>>> = Registry.BLOCK_ENTITY_TYPE_REGISTRY
    override val entityType: ResourceKey<Registry<EntityType<*>>> = Registry.ENTITY_TYPE_REGISTRY

}