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

        registerCommands(object {
            @Command("vnetwork analyser start")
            @ShortPerm("analyser")
            fun analyserStart(sender: User) {
                if (Analyser.start()) {
                    sender.message(locale, { analyser.started })
                } else {
                    sender.message(locale, { analyser.alreadyStarted })
                }
            }

            @Command("vnetwork analyser stop")
            @ShortPerm("analyser")
            fun analyserStop(sender: User) {
                if (Analyser.stop()) {
                    sender.message(locale, { analyser.stopped })
                } else {
                    sender.message(locale, { analyser.alreadyStopped })
                }
            }

            @Command("vnetwork analyser reset")
            @ShortPerm("analyser")
            fun analyserReset(sender: User) {
                Analyser.reset()
                sender.message(locale, { analyser.reset })
            }

            @Command("vnetwork analyser view")
            @ShortPerm("analyser")
            fun analyserView(sender: User,
                             @Flag("limit") limit: Int = 7,
                             @Flag("player") players: Player? = null,
                             @Flag("server") servers: RegisteredServer? = null) {
                val entries = Analyser.records
                    .let {
                        if (players != null)
                            it.mapValues { it.value.toList().filter { record -> players == record.player } }
                        else
                            it
                    }
                    .let {
                        if (servers != null)
                            it.mapValues { it.value.toList().filter { record -> servers == record.server } }
                        else
                            it
                    }
                    .filterValues { it.isNotEmpty() }
                    .mapValues {
                        val list = it.value.toList()
                        list.size to (list.sumOf { it.uncompressedSize.toLong() } to list.sumOf { it.compressedSize.toLong() })
                    }
                    .entries.sortedByDescending { it.value.second.second }
                if (entries.isEmpty()) {
                    sender.message(locale, { analyser.view.noData })
                    return
                }
                sender.message(locale, { analyser.view.header })
                for ((k, entry) in entries.take(limit)) {
                    val (counts, v) = entry
                    val (uncompressed, compressed) = v
                    sender.message(
                        locale, { analyser.view.entry },
                        unparsed("packet-type", k.name.lowercase()),
                        unparsed("counts", counts),
                        bytes(uncompressed, "uncompressed-size"),
                        bytes(compressed, "compressed-size"),
                    )
                }
                sender.message(
                    locale, { analyser.view.footer },
                    duration(
                        (if (Analyser.running) {
                            System.currentTimeMillis() - Analyser.startTime
                        } else {
                            Analyser.stopTime - Analyser.startTime
                        }).milliseconds, sender
                    ))
            }
        })
        Analyser // Init this
    }

    override fun disable() {
        super.disable()
        Injector.disable()
        Analyser.stop()
    }


    data class ModuleConfig(
        val a: Int = 1
    ): BaseModuleConfiguration() {
    }

    data class ModuleLang(
        val analyser: Analyser = Analyser(),
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
    }

}