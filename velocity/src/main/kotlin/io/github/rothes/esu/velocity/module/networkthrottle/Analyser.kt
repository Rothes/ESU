package io.github.rothes.esu.velocity.module.networkthrottle

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.velocitypowered.api.proxy.Player
import io.github.rothes.esu.velocity.module.networkthrottle.channel.ChannelHandler
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.module.networkthrottle.channel.PacketData
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
        startTime = System.currentTimeMillis()
        records.clear()
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

    data class PacketRecord(
        val player: Player?,
        val uncompressedSize: Int,
        val compressedSize: Int,
    )

    object EncoderHandler: ChannelHandler {

        override fun handle(packetData: PacketData) {
            val records = records.computeIfAbsent(packetData.packetType) { Collections.synchronizedList(arrayListOf()) }
            records.add(PacketRecord(packetData.player, packetData.uncompressedSize, packetData.compressedSize))
        }

    }

}