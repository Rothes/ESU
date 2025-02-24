package io.github.rothes.esu.velocity.module.networkthrottle

import com.velocitypowered.api.scheduler.ScheduledTask
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.amount
import io.github.rothes.esu.core.util.ComponentUtils.bytes
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.NetworkThrottleModule.locale
import io.github.rothes.esu.velocity.module.networkthrottle.channel.ChannelHandler
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.module.networkthrottle.channel.PacketData
import io.github.rothes.esu.velocity.plugin
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Commands
import org.incendo.cloud.annotations.Flag
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

object TrafficMonitor {

    private var users = linkedMapOf<User, Unit>()
    private var outgoingBytes = AtomicLong(0)
    private var outgoingPps = AtomicLong(0)
    private var task: ScheduledTask? = null

    fun enable() {
        task = plugin.server.scheduler.buildTask(plugin) { task ->
            val pps = outgoingPps.getAndSet(0)
            val bytes = outgoingBytes.getAndSet(0) + pps * 40 // TCP overhead. This is a rough estimate on the application layer.
            for ((user, unit) in users) {
                user.message(locale, { trafficMonitor.message },
                    unparsed("incoming-traffic", "N/A"),
                    when (unit) {
                        Unit.BIT -> bytes(bytes * 8, "outgoing-traffic", " Gbps", " Mbps", " Kbps", " bps")
                        Unit.BYTE -> bytes(bytes, "outgoing-traffic", " GiB/s", " MiB/s", " KiB/s", " B/s")
                    },
                    amount(pps, "outgoing-pps")
                )
            }
        }.repeat(Duration.ofSeconds(1)).schedule()
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
        if (users.isNotEmpty()) {
            users.clear()
            Injector.unregisterEncoderHandler(EncoderHandler)
        }
    }

    fun add(user: User, unit: Unit) {
        synchronized(users) {
            if (users.isEmpty()) Injector.registerEncoderHandler(EncoderHandler)
            users[user] = unit
        }
    }

    fun remove(user: User) {
        synchronized(users) {
            users.remove(user)
            if (users.isEmpty()) Injector.unregisterEncoderHandler(EncoderHandler)
        }
    }

    object EncoderHandler: ChannelHandler {

        override fun encode(packetData: PacketData) {
            outgoingBytes.addAndGet(packetData.compressedSize.toLong())
        }

        override fun flush() {
            outgoingPps.incrementAndGet()
        }

    }

    enum class Unit {
        BIT,
        BYTE,
    }
}