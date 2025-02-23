package io.github.rothes.esu.velocity.module.networkthrottle

import com.velocitypowered.api.scheduler.ScheduledTask
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.user.User
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
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

object TrafficMonitor {

    private var users = linkedSetOf<User>()
    private var outgoing = AtomicLong(0)
    private var task: ScheduledTask? = null

    fun enable() {
        task = plugin.server.scheduler.buildTask(plugin) { task ->
            val bytes = outgoing.getAndSet(0)
            for (user in users) {
                user.message(locale, { trafficMonitor.message },
                    unparsed("incoming-traffic", "N/A"),
                    bytes(bytes, "outgoing-traffic", " GiB/s", " MiB/s", " KiB/s", " B/s"),
                )
            }
        }.repeat(Duration.ofSeconds(1)).schedule()
        NetworkThrottleModule.registerCommands(object {
            @Commands(value = [Command("vnetwork trafficMonitor"), Command("vnetwork trafficMonitor toggle")])
            @ShortPerm("trafficMonitor")
            fun toggle(sender: User) {
                if (users.contains(sender)) {
                    disable(sender)
                } else {
                    enable(sender)
                }
            }
            @Command("vnetwork trafficMonitor enable")
            @ShortPerm("trafficMonitor")
            fun enable(sender: User) {
                add(sender)
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

    fun add(user: User) {
        synchronized(users) {
            if (users.isEmpty()) Injector.registerEncoderHandler(EncoderHandler)
            users.add(user)
        }
    }

    fun remove(user: User) {
        synchronized(users) {
            users.remove(user)
            if (users.isEmpty()) Injector.unregisterEncoderHandler(EncoderHandler)
        }
    }

    object EncoderHandler: ChannelHandler {

        override fun handle(packetData: PacketData) {
            outgoing.addAndGet(packetData.compressedSize.toLong())
        }

    }
}