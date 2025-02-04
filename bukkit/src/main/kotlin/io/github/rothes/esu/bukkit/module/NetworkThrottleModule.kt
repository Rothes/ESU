package io.github.rothes.esu.bukkit.module

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.incendo.cloud.annotations.Command
import kotlin.jvm.java
import kotlin.time.Duration.Companion.milliseconds

object NetworkThrottleModule: BukkitModule<NetworkThrottleModule.ModuleConfig, NetworkThrottleModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    override fun enable() {
        registerCommands(object {
            @Command("network analyser start")
            @ShortPerm("analyser")
            fun analyserStart(sender: User) {
                if (Analyser.start()) {
                    sender.message(locale, { analyser.started })
                } else {
                    sender.message(locale, { analyser.alreadyStarted })
                }
            }

            @Command("network analyser stop")
            @ShortPerm("analyser")
            fun analyserStop(sender: User) {
                if (Analyser.stop()) {
                    sender.message(locale, { analyser.stopped })
                } else {
                    sender.message(locale, { analyser.alreadyStopped })
                }
            }

            @Command("network analyser view")
            @ShortPerm("analyser")
            fun analyserView(sender: User) {
                val entries = Analyser.records
                    .mapValues { it.value.toList().sumOf { it.size.toLong() } }
                    .entries.sortedByDescending { it.value }
                if (entries.isEmpty()) {
                    sender.message(locale, { analyser.view.noData })
                    return
                }
                sender.message(locale, { analyser.view.header })
                for ((k, bytes) in entries.take(7)) {
                    sender.message(locale, { analyser.view.entry },
                        unparsed("packet-type", k.name.lowercase()),
                        unparsed("size", when {
                            bytes >= 1 shl 30 -> "%.1f GB".format(bytes.toDouble() / (1 shl 30))
                            bytes >= 1 shl 20 -> "%.1f MB".format(bytes.toDouble() / (1 shl 20))
                            bytes >= 1 shl 10 -> "%.1f KB".format(bytes.toDouble() / (1 shl 10))
                            else              -> "$bytes bytes"
                        }))
                }
                sender.message(locale, { analyser.view.footer },
                    duration(
                        (if (Analyser.running) {
                            System.currentTimeMillis() - Analyser.startTime
                        } else {
                            Analyser.stopTime - Analyser.startTime
                        }).milliseconds, sender
                    ))
            }
        })
    }

    override fun disable() {
        Analyser.stop()
    }

    object Analyser {

        var running: Boolean = false
            private set
        var startTime: Long = 0
            private set
        var stopTime: Long = 0
            private set

        val records = hashMapOf<PacketTypeCommon, MutableList<PacketRecord>>()

        fun start(): Boolean {
            if (running) return false
            running = true
            startTime = System.currentTimeMillis()
            records.clear()
            PacketEvents.getAPI().eventManager.registerListener(PacketListeners)
            return true
        }

        fun stop(): Boolean {
            if (!running) return false
            running = false
            stopTime = System.currentTimeMillis()
            PacketEvents.getAPI().eventManager.unregisterListener(PacketListeners)
            return true
        }

        data class PacketRecord(
            val size: Int,
        )

        object PacketListeners: PacketListenerAbstract(PacketListenerPriority.HIGHEST) {

            override fun onPacketSend(event: PacketSendEvent) {
                val records = records.computeIfAbsent(event.packetType) { arrayListOf() }
                records.add(PacketRecord(ByteBufHelper.capacity(event.byteBuf)))
            }
        }
    }


    data class ModuleConfig(
        val hi: Int = 0,
    ): BaseModuleConfiguration()

    data class ModuleLang(
        val analyser: Analyser = Analyser(),
    ): ConfigurationPart {

        data class Analyser(
            val started: MessageData = "<pc>Started the analyser.".message,
            val stopped: MessageData = "<pc>Stopped the analyser.".message,
            val alreadyStarted: MessageData = "<ec>The analyser is already running.".message,
            val alreadyStopped: MessageData = "<ec>The analyser is already stopped.".message,

            val view: View = View(),
        ) {

            data class View(
                val noData: MessageData = "<pc>There's no data for view.".message,
                val header: MessageData = "<pdc>[Packet Type]<pc>: <sc>[size]".message,
                val entry: MessageData = "<tdc><packet-type><tc>: <sdc><size>".message,
                val footer: MessageData = "<pc>The analyser has been running for <duration>".message,
            )
        }
    }



}