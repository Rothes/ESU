package io.github.rothes.esu.velocity.module

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.bytes
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.velocity.module.networkthrottle.Analyser
import io.github.rothes.esu.velocity.module.networkthrottle.TrafficMonitor
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.plugin
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import kotlin.time.Duration.Companion.milliseconds

object NetworkThrottleModule: VelocityModule<NetworkThrottleModule.ModuleConfig, NetworkThrottleModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    override fun enable() {
        if (plugin.server.pluginManager.getPlugin("packetevents") == null)
            error("PacketEvents is required!")
        Injector.enable()
        TrafficMonitor.enable()
        Analyser.enable()
    }

    override fun disable() {
        super.disable()
        Injector.disable()
        TrafficMonitor.disable()
        Analyser.disable()
    }


    data class ModuleConfig(
        val a: Int = 1
    ): BaseModuleConfiguration() {
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
                val header: MessageData = "<pdc>[Packet Type]<pc> <pc>[count]</pc>: <sc>[fin-size] <tc>/ [raw-size]</tc>".message,
                val entry: MessageData = "<tdc><packet-type> <pc>x<pdc><counts></pc><tc>: <sdc><compressed-size> <tc>/ <tdc><uncompressed-size".message,
                val footer: MessageData = "<pc>The analyser has been running for <duration>".message,
            )
        }

        data class TrafficMonitor(
            val message: MessageData = "<actionbar><#6AFFF3>⬇ <pc><incoming-traffic>  <tc>|  <#BF71FF>⬆ <pc><outgoing-traffic>".message,
            val enabled: MessageData = "<pc>You are now viewing traffic monitor.".message,
            val disabled: MessageData = "<pc>You are no longer viewing traffic monitor.".message,
        )
    }

}