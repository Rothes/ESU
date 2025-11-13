package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_17_1

import io.github.rothes.esu.bukkit.configuration.RegistryValueSerializer
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistries
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryValueSerializers
import io.leangen.geantyref.TypeToken
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

class MCRegistryValueSerializersImpl: MCRegistryValueSerializers {

    private val mcRegistryAccessHandler by Versioned(MCRegistryAccessHandler::class.java)
    private val mcRegistries by Versioned(MCRegistries::class.java)

    override val block = RegistryValueSerializer(mcRegistryAccessHandler, mcRegistries.block, object : TypeToken<Block>() {})
    override val blockEntityType = RegistryValueSerializer(mcRegistryAccessHandler, mcRegistries.blockEntityType, object : TypeToken<BlockEntityType<*>>() {})
    override val entityType = RegistryValueSerializer(mcRegistryAccessHandler, mcRegistries.entityType, object : TypeToken<EntityType<*>>() {})

}