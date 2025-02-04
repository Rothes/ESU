package io.github.rothes.esu.bukkit.module

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.spongepowered.configurate.objectmapping.meta.Comment
import kotlin.math.min
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
        Analyser // Init this
        PacketEvents.getAPI().eventManager.registerListener(PacketListener)
    }

    override fun disable() {
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListener)
        Analyser.stop()
    }

    object PacketListener: PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
        override fun onPacketSend(event: PacketSendEvent) {
            if (!config.chunkDataThrottle.enabled) {
                return
            }
            when (event.packetType) {
                PacketType.Play.Server.CHUNK_DATA -> {
                    val wrapper = WrapperPlayServerChunkData(event)
                    val column = wrapper.column
                    val world = event.getPlayer<Player>().world
                    val minHeight = world.minHeight
                    val maxHeight = world.maxHeight

                    val dp = Array(16) { Array(maxHeight - minHeight) { BooleanArray(16) } }

                    val chunks = column.chunks
                    for ((index, chunk) in chunks.withIndex()) {
                        for (x in 0 ..< 16) {
                            for (y in 0 ..< 16) {
                                val i = y + index * 16 - min(minHeight, 0)
                                out@ for (z in 0 ..< 16) {
                                    val get = chunk.get(event.user.clientVersion, x, y, z)
                                    val material = SpigotConversionUtil.toBukkitBlockData(get).material
                                    if (material.isOccluding) {
                                        dp[x][i][z] = true
                                        if ( x >= 2 && i >= 2 && z >= 2
                                            && dp[x - 2][i - 1][z - 1] && dp[x - 1][i - 2][z - 1] && dp[x - 1][i - 1][z - 2]
                                            && dp[x][i - 1][z - 1] && dp[x - 1][i - 1][z] && dp[x - 1][i][z - 1]) {

                                            if (y == 0) {
                                                chunks[index - 1]
                                                    .set(x - 1, 15   , z - 1, 0)
                                            } else {
                                                chunk
                                                    .set(x - 1, y - 1, z - 1, 0)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
            PacketEvents.getAPI().eventManager.registerListener(AnalyserPacketListener)
            return true
        }

        fun stop(): Boolean {
            if (!running) return false
            running = false
            stopTime = System.currentTimeMillis()
            PacketEvents.getAPI().eventManager.unregisterListener(AnalyserPacketListener)
            return true
        }

        data class PacketRecord(
            val size: Int,
        )

        object AnalyserPacketListener: PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            override fun onPacketSend(event: PacketSendEvent) {
                val records = records.computeIfAbsent(event.packetType) { arrayListOf() }
                records.add(PacketRecord(ByteBufHelper.capacity(event.byteBuf)))
            }
        }
    }


    data class ModuleConfig(
        @field:Comment("Helps to reduce chunk upload bandwidth.\n" +
                "Plugin will only send visible blocks if players are moving fast,\n" +
                "Once they interact with blocks, we send a full chunk data again.")
        val chunkDataThrottle: ChunkDataThrottle = ChunkDataThrottle(),
    ): BaseModuleConfiguration() {

        data class ChunkDataThrottle(
            val enabled: Boolean = false,
        )
    }

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