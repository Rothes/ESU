package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.RaytraceHandler
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration

object EntityCulling : CommonFeature<EntityCulling.FeatureConfig, EmptyConfiguration>() {

    init {
        if (ServerCompatibility.isPaper && ServerCompatibility.serverVersion >= 19)
            registerFeature(RaytraceHandler)
    }

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents() ?: let {
            if (!ServerCompatibility.isPaper || ServerCompatibility.serverVersion < 19) {
                return errFail { "This feature requires Paper 1.19+ .".message }
            }
            RaytraceHandler.checkConfig()
        }
    }

    override fun onEnable() {
    }

    @Comment("""
        Smart Occlusion Culling to save upload bandwidth.
        Plugin will hide invisible entities to players.
    """)
    class FeatureConfig: BaseFeatureConfiguration()

}