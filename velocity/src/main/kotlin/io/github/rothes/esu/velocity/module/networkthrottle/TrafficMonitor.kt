package io.github.rothes.esu.velocity.module.networkthrottle

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.scheduler.ScheduledTask
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.amount
import io.github.rothes.esu.core.util.ComponentUtils.bytes
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.NetworkThrottleModule.config
import io.github.rothes.esu.velocity.module.NetworkThrottleModule.lang
import io.github.rothes.esu.velocity.module.networkthrottle.channel.DecoderChannelHandler
import io.github.rothes.esu.velocity.module.networkthrottle.channel.EncoderChannelHandler
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.module.networkthrottle.channel.PacketData
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Commands
import org.incendo.cloud.annotations.Flag
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

object TrafficMonitor {

    private const val MIN_PAYLOAD = 46
    private const val MAX_PAYLOAD = 1500
    private const val ETHERNET_FRAME_OVERHEAD = 6 + 6 + 2 + 4 // MAC destination + MAC source + EtherType/length + CRC

    private var viewers = linkedMapOf<User, Unit>()
    private var task: ScheduledTask? = null
    private var forceRun = AtomicInteger(0)

    private var outgoingBytes = AtomicInteger(0)
    private var outgoingPps   = AtomicInteger(0)
    private var incomingBytes = AtomicInteger(0)
    private var incomingPps   = AtomicInteger(0)

    var lastOutgoingBytes = 0L
        private set
    var lastOutgoingPps   = 0
        private set
    var lastIncomingBytes = 0L
        private set
    var lastIncomingPps   = 0
        private set

    fun enable() {
        task = plugin.server.scheduler.buildTask(plugin.bootstrap) { _ ->
            val ppsO  = (outgoingPps.getAndSet(0) * config.trafficCalibration.outgoingPpsMultiplier).toInt()
            val bytesO = outgoingBytes.getAndSet(0).toLong()
            val ppsI  = (incomingPps.getAndSet(0) * config.trafficCalibration.incomingPpsMultiplier).toInt()
            val bytesI = incomingBytes.getAndSet(0).toLong()
            lastOutgoingPps = ppsO
            lastOutgoingBytes = bytesO
            lastIncomingPps = ppsI
            lastIncomingBytes = bytesI
            fun traffic(bytes: Long, key: String, unit: Unit) =
                when (unit) {
                    Unit.BIT  -> bytes(bytes * 8, key, " Gbps" , " Mbps" , " Kbps" , " bps")
                    Unit.BYTE -> bytes(bytes    , key, " GiB/s", " MiB/s", " KiB/s", " B/s")
                }

            for ((user, unit) in viewers) {
                user.message(lang, { trafficMonitor.message },
                    traffic(bytes = bytesO       , "outgoing-traffic", unit),
                    amount(amount = ppsO.toLong(), "outgoing-pps"),
                    traffic(bytes = bytesI       , "incoming-traffic", unit),
                    amount(amount = ppsI.toLong(), "incoming-pps"),
                )
            }
        }.repeat(Duration.ofSeconds(1)).schedule()
        NetworkThrottleModule.registerListener(Listeners)
        NetworkThrottleModule.registerCommands(object {
            @Commands(value = [Command("vnetwork trafficMonitor"), Command("vnetwork trafficMonitor toggle")])
            @ShortPerm("trafficMonitor")
            fun toggle(sender: User, @Flag("unit") unit: Unit = Unit.BIT) {
                if (!viewers.contains(sender)) {
                    enable(sender, unit)
                } else {
                    disable(sender)
                }
            }
            @Command("vnetwork trafficMonitor enable")
            @ShortPerm("trafficMonitor")
            fun enable(sender: User, @Flag("unit") unit: Unit = Unit.BIT) {
                addViewer(sender, unit)
                sender.message(lang, { trafficMonitor.enabled })
            }
            @Command("vnetwork trafficMonitor disable")
            @ShortPerm("trafficMonitor")
            fun disable(sender: User) {
                removeViewer(sender)
                sender.message(lang, { trafficMonitor.disabled })
            }
        })
    }

    fun disable() {
        task?.cancel()
        NetworkThrottleModule.unregisterListener(Listeners)
        if (viewers.isNotEmpty()) {
            viewers.clear()
            Injector.unregisterHandler(EncoderHandler)
            Injector.unregisterHandler(DecoderHandler)
        }
    }

    fun forceRecord() {
        if (forceRun.getAndAdd(1) == 0) {
            Injector.registerHandler(EncoderHandler)
            Injector.registerHandler(DecoderHandler)
        }
    }

    fun cancelForceRecord() {
        if (forceRun.addAndGet(-1) == 0 && viewers.isEmpty()) {
            Injector.unregisterHandler(EncoderHandler)
            Injector.unregisterHandler(DecoderHandler)
        }
    }

    fun addViewer(user: User, unit: Unit) {
        synchronized(viewers) {
            if (viewers.isEmpty()) {
                Injector.registerHandler(EncoderHandler)
                Injector.registerHandler(DecoderHandler)
            }
            viewers[user] = unit
        }
    }

    fun removeViewer(user: User) {
        synchronized(viewers) {
            viewers.remove(user)
            if (viewers.isEmpty() && forceRun.get() == 0) {
                Injector.unregisterHandler(EncoderHandler)
                Injector.unregisterHandler(DecoderHandler)
            }
        }
    }

    object Listeners {
        @Subscribe
        fun onDisconnect(e: DisconnectEvent) {
            removeViewer(e.player.user)
        }
    }

    object EncoderHandler: EncoderChannelHandler {

        override fun encode(packetData: PacketData) {
            val size = packetData.compressedSize
            outgoingBytes.getAndAdd(calculateEthernetFrameBytes(size))
        }

        override fun flush() {
            outgoingPps.getAndIncrement()
        }

    }

    object DecoderHandler: DecoderChannelHandler {

        override fun decode(packetData: PacketData) {
            val size = packetData.compressedSize
            incomingBytes.getAndAdd(calculateEthernetFrameBytes(size))
            incomingPps.getAndIncrement()
        }

    }

    private fun calculateEthernetFrameBytes(size: Int): Int {
        val frames = (size + (MAX_PAYLOAD - 1)) / (MAX_PAYLOAD)
        val overhead = frames * ETHERNET_FRAME_OVERHEAD
        val fills = MIN_PAYLOAD - (size - (frames - 1) * MAX_PAYLOAD)

        var ethernetFrameBytes = size + overhead
        if (fills > 0) ethernetFrameBytes += fills
        return ethernetFrameBytes
    }

    enum class Unit {
        BIT,
        BYTE,
    }
}