package io.github.rothes.esu.bukkit.util.version.adapter.nms.v17_1

import io.github.rothes.esu.bukkit.configuration.RegistryValueSerializer
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryValueSerializers
import io.leangen.geantyref.TypeToken
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType

object NmsRegistryValueSerializersImpl: NmsRegistryValueSerializers {

    private val nmsRegistryAccessHandler by Versioned(NmsRegistryAccessHandler::class.java)
    private val nmsRegistries by Versioned(NmsRegistries::class.java)

    override val biome = RegistryValueSerializer(nmsRegistryAccessHandler, nmsRegistries.biome, object : TypeToken<Biome>() {})
    override val block = RegistryValueSerializer(nmsRegistryAccessHandler, nmsRegistries.block, object : TypeToken<Block>() {})
    override val blockEntityType = RegistryValueSerializer(nmsRegistryAccessHandler, nmsRegistries.blockEntityType, object : TypeToken<BlockEntityType<*>>() {})
    override val entityType = RegistryValueSerializer(nmsRegistryAccessHandler, nmsRegistries.entityType, object : TypeToken<EntityType<*>>() {})

}