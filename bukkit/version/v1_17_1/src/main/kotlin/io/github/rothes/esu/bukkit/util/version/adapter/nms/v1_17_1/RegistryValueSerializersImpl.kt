package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_17_1

import io.github.rothes.esu.bukkit.configuration.RegistryValueSerializer
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.github.rothes.esu.bukkit.util.version.adapter.nms.RegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.RegistryValueSerializers
import io.leangen.geantyref.TypeToken
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

class RegistryValueSerializersImpl: RegistryValueSerializers {

    private val registryAccessHandler by Versioned(RegistryAccessHandler::class.java)
    private val nmsRegistries by Versioned(NmsRegistries::class.java)

    override val block = RegistryValueSerializer(registryAccessHandler, nmsRegistries.block, object : TypeToken<Block>() {})
    override val blockEntityType = RegistryValueSerializer(registryAccessHandler, nmsRegistries.blockEntityType, object : TypeToken<BlockEntityType<*>>() {})
    override val entityType = RegistryValueSerializer(registryAccessHandler, nmsRegistries.entityType, object : TypeToken<EntityType<*>>() {})

}