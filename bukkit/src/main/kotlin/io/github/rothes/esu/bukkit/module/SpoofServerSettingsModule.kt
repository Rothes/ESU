package io.github.rothes.esu.bukkit.module

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDifficulty
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateSimulationDistance
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateViewDistance
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import org.bukkit.Difficulty
import java.util.*
import kotlin.jvm.optionals.getOrNull

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
                        wrapper.difficulty = com.github.retrooper.packetevents.protocol.world.Difficulty.valueOf(it.name)
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