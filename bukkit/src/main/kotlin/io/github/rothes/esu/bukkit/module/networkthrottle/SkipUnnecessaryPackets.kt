package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration

object SkipUnnecessaryPackets: CommonFeature<SkipUnnecessaryPackets.FeatureConfig, EmptyConfiguration>() {

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents()
    }

    override fun onEnable() {
        PacketEvents.getAPI().eventManager.registerListener(PacketListeners)
    }

    override fun onDisable() {
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListeners)
    }

    private object PacketListeners: PacketListenerAbstract(PacketListenerPriority.LOWEST) {

        override fun onPacketSend(event: PacketSendEvent) {
            when (event.packetType) {
                PacketType.Play.Server.ENTITY_RELATIVE_MOVE -> {
                    val wrapper = WrapperPlayServerEntityRelativeMove(event)
                    if (wrapper.deltaX == 0.0 && wrapper.deltaY == 0.0 && wrapper.deltaZ == 0.0) {
                        event.isCancelled = true
                    }
                }
                PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION -> {
                    val wrapper = WrapperPlayServerEntityRelativeMoveAndRotation(event)
                    if (wrapper.deltaX == 0.0 && wrapper.deltaY == 0.0 && wrapper.deltaZ == 0.0
                        && wrapper.pitch == 0.0f && wrapper.yaw == 0.0f
                    ) {
                        event.isCancelled = true
                    }
                }
                PacketType.Play.Server.ENTITY_ROTATION -> {
                    val wrapper = WrapperPlayServerEntityRotation(event)
                    if (wrapper.pitch == 0.0f && wrapper.yaw == 0.0f) {
                        event.isCancelled = true
                    }
                }
            }
        }
    }

    @Comment("""
        This feature is to skip unnecessary packets, that doesn't bring any changes to client.
        Currently, it cancel entity movement packets whose entity actually not moved.
    """)
    class FeatureConfig: BaseFeatureConfiguration(true)
}