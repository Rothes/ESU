package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_17_1

import io.github.rothes.esu.bukkit.configuration.RegistryValueSerializer
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryValueSerializers
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.leangen.geantyref.TypeToken
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

class MCRegistryValueSerializersImpl: MCRegistryValueSerializers {

    private val mcRegistryAccessHandler by Versioned(MCRegistryAccessHandler::class.java)
    private val nmsRegistries by Versioned(NmsRegistries::class.java)

    override val block = RegistryValueSerializer(mcRegistryAccessHandler, nmsRegistries.block, object : TypeToken<Block>() {})
    override val blockEntityType = RegistryValueSerializer(mcRegistryAccessHandler, nmsRegistries.blockEntityType, object : TypeToken<BlockEntityType<*>>() {})
    override val entityType = RegistryValueSerializer(mcRegistryAccessHandler, nmsRegistries.entityType, object : TypeToken<EntityType<*>>() {})

}