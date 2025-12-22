package io.github.rothes.esu.bukkit.util.version.adapter.nms

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer

interface NmsRegistryValueSerializers {

    val biome: ScalarSerializer<*>
    val block: ScalarSerializer<*>
    val blockEntityType: ScalarSerializer<*>
    val entityType: ScalarSerializer<*>

    companion object {
        val isSupported = ServerCompatibility.serverVersion >= "17.1"
        val instance: NmsRegistryValueSerializers by lazy { NmsRegistryValueSerializers::class.java.versioned() }
    }

}