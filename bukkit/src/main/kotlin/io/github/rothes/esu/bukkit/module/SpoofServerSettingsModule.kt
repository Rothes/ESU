package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.lib.packetevents.PacketEvents
import io.github.rothes.esu.lib.packetevents.event.PacketListenerAbstract
import io.github.rothes.esu.lib.packetevents.event.PacketListenerPriority
import io.github.rothes.esu.lib.packetevents.event.PacketSendEvent
import io.github.rothes.esu.lib.packetevents.protocol.packettype.PacketType
import io.github.rothes.esu.lib.packetevents.wrapper.play.server.WrapperPlayServerDifficulty
import io.github.rothes.esu.lib.packetevents.wrapper.play.server.WrapperPlayServerUpdateSimulationDistance
import io.github.rothes.esu.lib.packetevents.wrapper.play.server.WrapperPlayServerUpdateViewDistance
import org.bukkit.Difficulty
import java.util.*
import kotlin.jvm.optionals.getOrNull

object SpoofServerSettingsModule: BukkitModule<SpoofServerSettingsModule.ModuleConfig, EmptyConfiguration>() {

    override fun onEnable() {
        PacketEvents.getAPI().eventManager.registerListener(PacketListeners)
    }

    override fun onDisable() {
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListeners)
    }

    object PacketListeners: PacketListenerAbstract(PacketListenerPriority.HIGHEST) {

        override fun onPacketSend(event: PacketSendEvent) {
            when (event.packetType) {
                PacketType.Play.Server.UPDATE_VIEW_DISTANCE       -> {
                    val wrapper = WrapperPlayServerUpdateViewDistance(event)
                    config.viewDistance.getOrNull()?.let {
                        wrapper.viewDistance = it
                    }
                }
                PacketType.Play.Server.UPDATE_SIMULATION_DISTANCE -> {
                    val wrapper = WrapperPlayServerUpdateSimulationDistance(event)
                    config.simulationDistance.getOrNull()?.let {
                        wrapper.simulationDistance = it
                    }
                }
                PacketType.Play.Server.SERVER_DIFFICULTY -> {
                    val wrapper = WrapperPlayServerDifficulty(event)
                    config.difficulty.getOrNull()?.let {
                        wrapper.difficulty = io.github.rothes.esu.lib.packetevents.protocol.world.Difficulty.valueOf(it.name)
                    }
                }
            }
        }
    }

    data class ModuleConfig(
        val viewDistance: Optional<Int> = Optional.empty(),
        val simulationDistance: Optional<Int> = Optional.empty(),
        val difficulty: Optional<Difficulty> = Optional.empty(),
    ): BaseModuleConfiguration()
}