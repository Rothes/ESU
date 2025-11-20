package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.networkthrottle.*
import io.github.rothes.esu.bukkit.util.version.VersionUtils.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryValueSerializers
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import java.util.*

object NetworkThrottleModule: BukkitModule<BaseModuleConfiguration, NetworkThrottleModule.ModuleLang>() {

    init {
        registerFeature(ChunkDataThrottle)
        registerFeature(DynamicChunkSendRate)
        registerFeature(EntityCulling)
        if (MCRegistryValueSerializers.isSupported) registerFeature(EntityUpdateInterval::class.java.versioned())
        registerFeature(HighLatencyAdjust)
        registerFeature(SkipUnnecessaryPackets)
    }

    lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override fun onEnable() {
        data = ConfigLoader.load(dataPath)
    }

    override fun onDisable() {
        super.onDisable()
        ConfigLoader.save(dataPath, data)
    }

    override fun buildConfigLoader(builder: YamlConfigurationLoader.Builder) {
        if (MCRegistryValueSerializers.isSupported) {
            builder.defaultOptions { options ->
                options.serializers { builder ->
                    builder.register(
                        MCRegistryValueSerializers.instance.entityType
                    )
                }
            }
        }
    }


    data class ModuleData(
        val originalViewDistance: MutableMap<UUID, Int> = linkedMapOf(),
    )

    class ModuleLang(): ConfigurationPart

}


