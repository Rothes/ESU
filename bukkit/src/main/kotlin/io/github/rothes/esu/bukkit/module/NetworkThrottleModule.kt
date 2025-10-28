package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.networkthrottle.ChunkDataThrottle
import io.github.rothes.esu.bukkit.module.networkthrottle.DynamicChunkSendRate
import io.github.rothes.esu.bukkit.module.networkthrottle.HighLatencyAdjust
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import java.util.*

object NetworkThrottleModule: BukkitModule<BaseModuleConfiguration, NetworkThrottleModule.ModuleLang>() {

    init {
        registerFeature(ChunkDataThrottle)
        registerFeature(DynamicChunkSendRate)
        registerFeature(HighLatencyAdjust)
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


    data class ModuleData(
        val originalViewDistance: MutableMap<UUID, Int> = linkedMapOf(),
    )

    class ModuleLang(): ConfigurationPart

}


