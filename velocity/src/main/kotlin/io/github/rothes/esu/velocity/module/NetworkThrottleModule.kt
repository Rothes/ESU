package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.velocity.module.networkthrottle.Analyser
import io.github.rothes.esu.velocity.module.networkthrottle.DynamicChunkSendRate
import io.github.rothes.esu.velocity.module.networkthrottle.TrafficMonitor
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.plugin
import org.spongepowered.configurate.objectmapping.meta.Comment
import kotlin.jvm.java

object NetworkThrottleModule: VelocityModule<NetworkThrottleModule.ModuleConfig, NetworkThrottleModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    override fun enable() {
        if (plugin.server.pluginManager.getPlugin("packetevents") == null)
            error("PacketEvents is required!")
        Injector.enable()
        TrafficMonitor.enable()
        Analyser.enable()
        DynamicChunkSendRate.enable()
    }

    override fun disable() {
        super.disable()
        Injector.disable()
        TrafficMonitor.disable()
        Analyser.disable()
        DynamicChunkSendRate.disable()
    }

    override fun reloadConfig() {
        super.reloadConfig()
        if (config.dynamicChunkSendRate.enabled) {
            DynamicChunkSendRate.enable()
        } else {
            DynamicChunkSendRate.disable()
        }
    }


    data class ModuleConfig(
        @field:Comment("Dynamic manage chunk send rate with outgoing traffic.\n" +
                "NetworkThrottleModule at backend servers enabled is required.")
        val dynamicChunkSendRate: DynamicChunkSendRate = DynamicChunkSendRate(),
        @field:Comment("We can't know exactly what the actual bandwidth rate and packet rate are at netty level.\n" +
                "You can modify the calibration parameters here, for advanced users.")
        val trafficCalibration: TrafficCalibration = TrafficCalibration(),
    ): BaseModuleConfiguration() {

        data class DynamicChunkSendRate(
            val enabled: Boolean = true,
//            @field:Comment("The total upload bandwidth this server can send. Unit is Kbps.")
//            val totalUploadBandwidth: Long = 50 * 1024,
            @field:Comment("The upload bandwidth threshold to start the throttle. Unit is Kbps.\n" +
                    "We will use the data from traffic monitor, so you may have done the\n" +
                    " traffic-calibration settings.")
            val limitUploadBandwidth: Long = 50 * 1024,
            @field:Comment("Minimum guaranteed rate per player. If server bandwidth hits limitUploadBandwidth,\n" +
                    "player with outgoing rates above this will be limit chunk sending by one second immediately.\n" +
                    "This helps reduce the probability of spikes, and distributes the bandwidth evenly.\n" +
                    "It's not easy to hit the default value, you may set it lower like 1520 if you have\n" +
                    " many players.")
            val guaranteedBandwidth: Long = 2048,
        )

        data class TrafficCalibration(
            val outgoingPpsMultiplier: Double = 0.6667,
            val incomingPpsMultiplier: Double = 3.0,
        )
    }

    data class ModuleLang(
        val analyser: Analyser = Analyser(),
        val trafficMonitor: TrafficMonitor = TrafficMonitor(),
    ): ConfigurationPart {

        data class Analyser(
            val started: MessageData = "<pc>Started the analyser.".message,
            val stopped: MessageData = "<pc>Stopped the analyser.".message,
            val reset: MessageData = "<pc>Reset the analyser.".message,
            val alreadyStarted: MessageData = "<ec>The analyser is already running.".message,
            val alreadyStopped: MessageData = "<ec>The analyser is already stopped.".message,

            val view: View = View(),
        ) {

            data class View(
                val noData: MessageData = "<pc>There's no data for view.".message,
                val header: MessageData = "<pdc>[Packet Type]<pc> <pc>[count]</pc>: <sc>[size] <tc>/ [raw-size]</tc>".message,
                val entry: MessageData = "<tdc><packet-type> <pc>x<pdc><counts></pc><tc>: <sdc><compressed-size> <tc>/ <tdc><uncompressed-size>".message,
                val footer: MessageData = "<pc>The analyser has been running for <duration>".message,
            )
        }

        data class TrafficMonitor(
            val message: MessageData = "<actionbar><pdc><b>ESU</b> Monitor <tc>-  <#BF71FF>⬆ <pc><outgoing-traffic>  <outgoing-pps>pps  <tc>|  <#6AFFF3>⬇ <pc><incoming-traffic>  <incoming-pps>pps".message,
            val enabled: MessageData = "<pc>You are now viewing traffic monitor.".message,
            val disabled: MessageData = "<pc>You are no longer viewing traffic monitor.<actionbar>".message,
        )
    }

}