package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.velocity.module.networkthrottle.Analyser
import io.github.rothes.esu.velocity.module.networkthrottle.DynamicChunkSendRate
import io.github.rothes.esu.velocity.module.networkthrottle.TrafficMonitor
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.util.extension.checkPacketEvents

object NetworkThrottleModule: VelocityModule<NetworkThrottleModule.ModuleConfig, NetworkThrottleModule.ModuleLang>() {

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents()
    }

    override fun onEnable() {
        Injector.enable()
        TrafficMonitor.enable()
        Analyser.enable()
        DynamicChunkSendRate.enable()
    }

    override fun onDisable() {
        super.onDisable()
        Injector.disable()
        TrafficMonitor.disable()
        Analyser.disable()
        Analyser.reset()
        DynamicChunkSendRate.disable()
    }

    override fun onReload() {
        super.onReload()
        if (config.dynamicChunkSendRate.enabled) {
            DynamicChunkSendRate.enable()
        } else {
            DynamicChunkSendRate.disable()
        }
    }


    data class ModuleConfig(
        @Comment("Dynamic manage chunk send rate with outgoing traffic.\n" +
                "NetworkThrottleModule at backend servers enabled is required.")
        val dynamicChunkSendRate: DynamicChunkSendRate = DynamicChunkSendRate(),
        @Comment("We can't know exactly what the actual bandwidth rate and packet rate are at netty level.\n" +
                "You can modify the calibration parameters here, for advanced users.")
        val trafficCalibration: TrafficCalibration = TrafficCalibration(),
    ): BaseModuleConfiguration() {

        data class DynamicChunkSendRate(
            val enabled: Boolean = true,
//            @Comment("The total upload bandwidth this server can send. Unit is Kbps.")
//            val totalUploadBandwidth: Long = 50 * 1024,
            @Comment("The upload bandwidth threshold to start the throttle. Unit is Kbps.\n" +
                    "We will use the data from traffic monitor, so you may have done the\n" +
                    " traffic-calibration settings.")
            val limitUploadBandwidth: Long = 50 * 1024,
            @Comment("Minimum guaranteed rate per player. If server bandwidth hits limitUploadBandwidth,\n" +
                    "player with outgoing rates above this will be limit chunk sending by one second immediately.\n" +
                    "This helps reduce the probability of spikes, and distributes the bandwidth evenly.\n" +
                    "It's not easy to hit the default value, you may set it lower like 1520 if you have\n" +
                    " many players.")
            val guaranteedBandwidth: Long = 2048,
        )

        data class TrafficCalibration(
            val outgoingPpsMultiplier: Double = 1.0,
            val incomingPpsMultiplier: Double = 1.0,
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