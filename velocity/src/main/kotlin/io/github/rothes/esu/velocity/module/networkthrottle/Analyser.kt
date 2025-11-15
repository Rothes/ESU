package io.github.rothes.esu.velocity.module.networkthrottle

import com.github.retrooper.packetevents.protocol.PacketSide
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.bytes
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.NetworkThrottleModule.lang
import io.github.rothes.esu.velocity.module.networkthrottle.channel.DecoderChannelHandler
import io.github.rothes.esu.velocity.module.networkthrottle.channel.EncoderChannelHandler
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
        Injector.registerHandler(EncoderHandler)
        Injector.registerHandler(DecoderHandler)
        return true
    }

    fun enable() {
        NetworkThrottleModule.registerCommands(object {
            @Command("vnetwork analyser start")
            @ShortPerm("analyser")
            fun analyserStart(sender: User) {
                if (start()) {
                    sender.message(lang, { analyser.started })
                } else {
                    sender.message(lang, { analyser.alreadyStarted })
                }
            }

            @Command("vnetwork analyser stop")
            @ShortPerm("analyser")
            fun analyserStop(sender: User) {
                if (disable()) {
                    sender.message(lang, { analyser.stopped })
                } else {
                    sender.message(lang, { analyser.alreadyStopped })
                }
            }

            @Command("vnetwork analyser reset")
            @ShortPerm("analyser")
            fun analyserReset(sender: User) {
                reset()
                sender.message(lang, { analyser.reset })
            }

            @Command("vnetwork analyser view")
            @ShortPerm("analyser")
            fun analyserView(sender: User,
                             @Flag("side") side: PacketSide? = null,
                             @Flag("player") player: Player? = null,
                             @Flag("server") server: RegisteredServer? = null,
                             @Flag("limit") limit: Int = 7, ) {
                val entries = records.mapValues { LinkedList(it.value) }
                    .let { map ->
                        if (side != null)
                            map.filterKeys { it.side == side }
                        else
                            map
                    }
                    .also { map ->
                        if (player != null)
                            map.values.forEach { it.removeIf { record -> player != record.player } }
                        if (server != null)
                            map.values.forEach { it.removeIf { record -> server != record.server } }
                    }
                    .filterValues { it.isNotEmpty() }
                    .mapValues { entry ->
                        val list = entry.value
                        list.size to (list.sumOf { it.uncompressedSize.toLong() } to list.sumOf { it.compressedSize.toLong() })
                    }
                    .entries.sortedByDescending { it.value.second.second }
                if (entries.isEmpty()) {
                    sender.message(lang, { analyser.view.noData })
                    return
                }
                sender.message(lang, { analyser.view.header })
                for ((k, entry) in entries.take(limit)) {
                    val (counts, v) = entry
                    val (uncompressed, compressed) = v
                    sender.message(
                        lang, { analyser.view.entry },
                        unparsed("packet-type", k.name.lowercase()),
                        unparsed("counts", counts),
                        bytes(uncompressed, "uncompressed-size"),
                        bytes(compressed, "compressed-size"),
                    )
                }
                sender.message(
                    lang, { analyser.view.footer },
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
        Injector.unregisterHandler(EncoderHandler)
        Injector.unregisterHandler(DecoderHandler)
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

    object EncoderHandler: EncoderChannelHandler {

        override fun encode(packetData: PacketData) {
            val records = records.computeIfAbsent(packetData.packetType) { Collections.synchronizedList(arrayListOf()) }
            val player = packetData.player
            records.add(PacketRecord(player, player?.currentServer?.getOrNull()?.server, packetData.uncompressedSize, packetData.compressedSize))
        }

    }

    object DecoderHandler: DecoderChannelHandler {

        override fun decode(packetData: PacketData) {
            val records = records.computeIfAbsent(packetData.packetType) { Collections.synchronizedList(arrayListOf()) }
            val player = packetData.player
            records.add(PacketRecord(player, player?.currentServer?.getOrNull()?.server, packetData.uncompressedSize, packetData.compressedSize))
        }

    }

}