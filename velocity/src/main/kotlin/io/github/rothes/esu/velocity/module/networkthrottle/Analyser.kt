package io.github.rothes.esu.velocity.module.networkthrottle

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.bytes
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.NetworkThrottleModule.locale
import io.github.rothes.esu.velocity.module.networkthrottle.channel.ChannelHandler
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.module.networkthrottle.channel.PacketData
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.milliseconds

object Analyser {

    var running: Boolean = false
        private set
    var startTime: Long = 0
        private set
    var stopTime: Long = 0
        private set

    val records = ConcurrentHashMap<PacketTypeCommon, MutableList<PacketRecord>>()

    fun start(): Boolean {
        if (running) return false
        running = true
        reset()
        Injector.registerEncoderHandler(EncoderHandler)
        return true
    }

    fun enable() {
        NetworkThrottleModule.registerCommands(object {
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
                if (Analyser.disable()) {
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
                        (if (running) {
                            System.currentTimeMillis() - startTime
                        } else {
                            stopTime - startTime
                        }).milliseconds, sender
                    ))
            }
        })
    }

    fun disable(): Boolean {
        if (!running) return false
        running = false
        stopTime = System.currentTimeMillis()
        Injector.unregisterEncoderHandler(EncoderHandler)
        return true
    }

    fun reset() {
        startTime = System.currentTimeMillis()
        records.clear()
    }

    data class PacketRecord(
        val player: Player?,
        val server: RegisteredServer?,
        val uncompressedSize: Int,
        val compressedSize: Int,
    )

    object EncoderHandler: ChannelHandler {

        override fun encode(packetData: PacketData) {
            val records = records.computeIfAbsent(packetData.packetType) { Collections.synchronizedList(arrayListOf()) }
            val player = packetData.player
            records.add(PacketRecord(player, player?.currentServer?.getOrNull()?.server, packetData.uncompressedSize, packetData.compressedSize))
        }

    }

}