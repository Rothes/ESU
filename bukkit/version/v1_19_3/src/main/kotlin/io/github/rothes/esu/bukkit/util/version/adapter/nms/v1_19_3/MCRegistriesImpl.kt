package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_19_3

import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistries
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

object MCRegistriesImpl: MCRegistries {

    override val block: ResourceKey<Registry<Block>> = Registries.BLOCK
    override val blockEntityType: ResourceKey<Registry<BlockEntityType<*>>> = Registries.BLOCK_ENTITY_TYPE
    override val entityType: ResourceKey<Registry<EntityType<*>>> = Registries.ENTITY_TYPE

}