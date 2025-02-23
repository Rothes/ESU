package io.github.rothes.esu.velocity.module.networkthrottle

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.github.rothes.esu.velocity.module.networkthrottle.channel.ChannelHandler
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.module.networkthrottle.channel.PacketData
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

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

    fun stop(): Boolean {
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

        override fun handle(packetData: PacketData) {
            val records = records.computeIfAbsent(packetData.packetType) { Collections.synchronizedList(arrayListOf()) }
            val player = packetData.player
            records.add(PacketRecord(player, player?.currentServer?.getOrNull()?.server, packetData.uncompressedSize, packetData.compressedSize))
        }

    }

}