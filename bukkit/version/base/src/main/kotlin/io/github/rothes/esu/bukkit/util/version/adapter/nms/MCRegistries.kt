package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

interface MCRegistries {

    val block: ResourceKey<Registry<Block>>
    val blockEntityType: ResourceKey<Registry<BlockEntityType<*>>>
    val entityType: ResourceKey<Registry<EntityType<*>>>

}