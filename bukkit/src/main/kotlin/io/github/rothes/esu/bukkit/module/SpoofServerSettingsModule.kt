package io.github.rothes.esu.bukkit.module

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateSimulationDistance
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateViewDistance
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration

object SpoofServerSettingsModule: BukkitModule<SpoofServerSettingsModule.ModuleConfig, EmptyConfiguration>(
    ModuleConfig::class.java, EmptyConfiguration::class.java
) {

    override fun enable() {
        PacketEvents.getAPI().eventManager.registerListener(PacketListeners)
    }

    override fun disable() {
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListeners)
    }

    object PacketListeners: PacketListenerAbstract(PacketListenerPriority.HIGHEST) {

        override fun onPacketSend(event: PacketSendEvent) {
            when (event.packetType) {
                PacketType.Play.Server.UPDATE_VIEW_DISTANCE -> {
                    val wrapper = WrapperPlayServerUpdateViewDistance(event)
                    if (config.viewDistance >= 0) {
                        wrapper.viewDistance = config.viewDistance
                    }
                }
                PacketType.Play.Server.UPDATE_SIMULATION_DISTANCE -> {
                    val wrapper = WrapperPlayServerUpdateSimulationDistance(event)
                    if (config.simulationDistance >= 0) {
                        wrapper.simulationDistance = config.simulationDistance
                    }
                }
            }
        }
    }

    data class ModuleConfig(
        val viewDistance: Int = -1,
        val simulationDistance: Int = -1,
    ): BaseModuleConfiguration()
}