package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_17_1

import io.github.rothes.esu.bukkit.configuration.RegistryValueSerializer
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.RegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.RegistryValueSerializers
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.leangen.geantyref.TypeToken
import net.minecraft.core.Registry
import net.minecraft.world.entity.EntityType

class RegistryValueSerializersImpl: RegistryValueSerializers {

    private val registryAccessHandler by Versioned(RegistryAccessHandler::class.java)

    override val entityType: ScalarSerializer<*> = RegistryValueSerializer(registryAccessHandler, Registry.ENTITY_TYPE_REGISTRY, object : TypeToken<EntityType<*>>() {})

}