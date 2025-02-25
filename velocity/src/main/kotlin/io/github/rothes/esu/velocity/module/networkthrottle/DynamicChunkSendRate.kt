package io.github.rothes.esu.velocity.module.networkthrottle

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.scheduler.ScheduledTask
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.NetworkThrottleModule.config
import io.github.rothes.esu.velocity.module.networkthrottle.channel.EncoderChannelHandler
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.module.networkthrottle.channel.PacketData
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.util.DataSerializer.encode
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.jvm.optionals.getOrNull

object DynamicChunkSendRate {

    private val CHANNEL_IDENTIFIER = MinecraftChannelIdentifier.create("esu", "dynamic_chunk_send_rate")

    private val traffic = ConcurrentHashMap<Player, AtomicLong>()
    private var task: ScheduledTask? = null

    val running get() = task != null

    fun enable() {
        NetworkThrottleModule.registerListener(Listeners)
        if (config.dynamicChunkSendRate.enabled && !running) {
            task = plugin.server.scheduler.buildTask(plugin) { task ->
                val total = TrafficMonitor.lastOutgoingBytes shr 10
                val limitUploadBandwidth = config.dynamicChunkSendRate.limitUploadBandwidth
                val guaranteedBandwidth = config.dynamicChunkSendRate.guaranteedBandwidth

                val limit = arrayListOf<Player>()
                for ((player, atomicLong) in traffic) {
                    val outgoing = atomicLong.getAndSet(0) shr 10
                    if (total >= limitUploadBandwidth && outgoing >= guaranteedBandwidth) {
                        limit.add(player)
                    }
                }
                for ((server, players) in limit.groupBy { it.currentServer.getOrNull() }) {
                    server?.sendPluginMessage(CHANNEL_IDENTIFIER, players.map { it.uniqueId }.encode())
                }
            }.repeat(Duration.ofSeconds(1)).schedule()
            TrafficMonitor.forceRecord()
            Injector.registerHandler(EncoderHandler)
        }
    }

    fun disable() {
        if (running) {
            task?.cancel()
            task = null
            TrafficMonitor.cancelForceRecord()
            Injector.unregisterHandler(EncoderHandler)
            traffic.clear()
        }
    }

    object Listeners {
        @Subscribe
        fun onMessage(e: PluginMessageEvent) {
            if (e.identifier == CHANNEL_IDENTIFIER && e.source is Player) {
                // We don't want players to fake the message
                e.result = PluginMessageEvent.ForwardResult.handled()
            }
        }

        @Subscribe
        fun onDisconnect(e: DisconnectEvent) {
            traffic.remove(e.player)
        }

    }

    object EncoderHandler: EncoderChannelHandler {

        override fun encode(packetData: PacketData) {
            val player = packetData.player ?: return
            val atomicLong = traffic.computeIfAbsent(player) { AtomicLong(0) }
            atomicLong.addAndGet(packetData.compressedSize.toLong() + (46 / 3).toLong())
        }

    }
}