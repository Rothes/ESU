package io.github.rothes.esu.bukkit.util.version.adapter.nms

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.VersionUtils.versioned
import io.github.rothes.esu.core.util.version.Version
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer

interface MCRegistryValueSerializers {

    val block: ScalarSerializer<*>
    val blockEntityType: ScalarSerializer<*>
    val entityType: ScalarSerializer<*>

    companion object {
        val isSupported = ServerCompatibility.serverVersion >= Version.fromString("1.17.1")
        val instance: MCRegistryValueSerializers by lazy { MCRegistryValueSerializers::class.java.versioned() }
    }

}