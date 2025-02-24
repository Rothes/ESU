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
import io.github.rothes.esu.velocity.module.NetworkThrottleModule.locale
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
import java.util.concurrent.atomic.AtomicLong

object TrafficMonitor {

    private var viewers = linkedMapOf<User, Unit>()
    private var task: ScheduledTask? = null
    private var forceRun = AtomicInteger(0)

    private var outgoingBytes = AtomicLong(0)
    private var outgoingPps   = AtomicLong(0)
    private var incomingBytes = AtomicLong(0)
    private var incomingPps   = AtomicLong(0)

    var lastOutgoingBytes = 0L
        private set
    var lastOutgoingPps   = 0L
        private set
    var lastIncomingBytes = 0L
        private set
    var lastIncomingPps   = 0L
        private set

    fun enable() {
        task = plugin.server.scheduler.buildTask(plugin) { task ->
            val ppsO  = (outgoingPps.getAndSet(0) * config.trafficCalibration.outgoingPpsMultiplier).toLong()
            val bytesO = outgoingBytes.getAndSet(0) + ppsO * 46 // TCP overhead.
            val ppsI  = (incomingPps.getAndSet(0) * config.trafficCalibration.incomingPpsMultiplier).toLong()
            val bytesI = incomingBytes.getAndSet(0) + ppsI * 46
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
                user.message(locale, { trafficMonitor.message },
                    traffic(bytesO, "outgoing-traffic", unit),
                    amount( ppsO  , "outgoing-pps"),
                    traffic(bytesI, "incoming-traffic", unit),
                    amount( ppsI  , "incoming-pps"),
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
                sender.message(locale, { trafficMonitor.enabled })
            }
            @Command("vnetwork trafficMonitor disable")
            @ShortPerm("trafficMonitor")
            fun disable(sender: User) {
                removeViewer(sender)
                sender.message(locale, { trafficMonitor.disabled })
            }
        })
    }

    fun disable() {
        task?.cancel()
        NetworkThrottleModule.unregisterListener(Listeners)
        if (viewers.isNotEmpty()) {
            viewers.clear()
            Injector.unregisterEncoderHandler(EncoderHandler)
            Injector.unregisterDecoderHandler(DecoderHandler)
        }
    }

    fun forceRecord() {
        if (forceRun.getAndAdd(1) == 0) {
            Injector.registerEncoderHandler(EncoderHandler)
            Injector.registerDecoderHandler(DecoderHandler)
        }
    }

    fun removeForceRecord() {
        if (forceRun.addAndGet(-1) == 0 && viewers.isEmpty()) {
            Injector.unregisterEncoderHandler(EncoderHandler)
            Injector.unregisterDecoderHandler(DecoderHandler)
        }
    }

    fun addViewer(user: User, unit: Unit) {
        synchronized(viewers) {
            if (viewers.isEmpty()) {
                Injector.registerEncoderHandler(EncoderHandler)
                Injector.registerDecoderHandler(DecoderHandler)
            }
            viewers[user] = unit
        }
    }

    fun removeViewer(user: User) {
        synchronized(viewers) {
            viewers.remove(user)
            if (viewers.isEmpty() && forceRun.get() == 0) {
                Injector.unregisterEncoderHandler(EncoderHandler)
                Injector.unregisterDecoderHandler(DecoderHandler)
            }
        }
    }

    object Listeners {
        @Subscribe
        fun onDisconnect(e: DisconnectEvent) {
            viewers.remove(e.player.user)
        }
    }

    object EncoderHandler: EncoderChannelHandler {

        override fun encode(packetData: PacketData) {
            outgoingBytes.addAndGet(packetData.compressedSize.toLong())
        }

        override fun flush() {
            outgoingPps.incrementAndGet()
        }

    }

    object DecoderHandler: DecoderChannelHandler {

        override fun decode(packetData: PacketData) {
            incomingBytes.addAndGet(packetData.compressedSize.toLong())
            incomingPps.incrementAndGet()
        }

    }

    enum class Unit {
        BIT,
        BYTE,
    }
}