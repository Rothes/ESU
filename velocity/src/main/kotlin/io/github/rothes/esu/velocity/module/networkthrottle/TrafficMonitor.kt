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
import java.util.concurrent.atomic.AtomicLong

object TrafficMonitor {

    private var incomingBytes = AtomicLong(0)
    private var incomingPps   = AtomicLong(0)
    private var outgoingBytes = AtomicLong(0)
    private var outgoingPps   = AtomicLong(0)

    private var users = linkedMapOf<User, Unit>()
    private var task: ScheduledTask? = null

    fun enable() {
        task = plugin.server.scheduler.buildTask(plugin) { task ->
            val ppsO  = (outgoingPps.getAndSet(0) * config.trafficCalibration.outgoingPpsMultiplier).toLong()
            val bytesO = outgoingBytes.getAndSet(0) + ppsO * 46 // TCP overhead.
            val ppsI  = (incomingPps.getAndSet(0) * config.trafficCalibration.incomingPpsMultiplier).toLong()
            val bytesI = incomingBytes.getAndSet(0) + ppsI * 46
            fun traffic(bytes: Long, key: String, unit: Unit) =
                when (unit) {
                    Unit.BIT  -> bytes(bytes * 8, key, " Gbps" , " Mbps" , " Kbps" , " bps")
                    Unit.BYTE -> bytes(bytes    , key, " GiB/s", " MiB/s", " KiB/s", " B/s")
                }

            for ((user, unit) in users) {
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
                if (!users.contains(sender)) {
                    enable(sender, unit)
                } else {
                    disable(sender)
                }
            }
            @Command("vnetwork trafficMonitor enable")
            @ShortPerm("trafficMonitor")
            fun enable(sender: User, @Flag("unit") unit: Unit = Unit.BIT) {
                add(sender, unit)
                sender.message(locale, { trafficMonitor.enabled })
            }
            @Command("vnetwork trafficMonitor disable")
            @ShortPerm("trafficMonitor")
            fun disable(sender: User) {
                remove(sender)
                sender.message(locale, { trafficMonitor.disabled })
            }
        })
    }

    fun disable() {
        task?.cancel()
        NetworkThrottleModule.unregisterListener(Listeners)
        if (users.isNotEmpty()) {
            users.clear()
            Injector.unregisterEncoderHandler(EncoderHandler)
            Injector.unregisterDecoderHandler(DecoderHandler)
        }
    }

    fun add(user: User, unit: Unit) {
        synchronized(users) {
            if (users.isEmpty()) {
                Injector.registerEncoderHandler(EncoderHandler)
                Injector.registerDecoderHandler(DecoderHandler)
            }
            users[user] = unit
        }
    }

    fun remove(user: User) {
        synchronized(users) {
            users.remove(user)
            if (users.isEmpty()) {
                Injector.unregisterEncoderHandler(EncoderHandler)
                Injector.unregisterDecoderHandler(DecoderHandler)
            }
        }
    }

    object Listeners {
        @Subscribe
        fun onDisconnect(e: DisconnectEvent) {
            users.remove(e.player.user)
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