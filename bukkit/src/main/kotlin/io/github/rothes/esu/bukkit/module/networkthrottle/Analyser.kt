package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import io.netty.buffer.ByteBuf
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

    object AnalyserPacketListener: PacketListenerAbstract(PacketListenerPriority.MONITOR) {
        override fun onPacketSend(event: PacketSendEvent) {
            val records = records.computeIfAbsent(event.packetType) { Collections.synchronizedList(arrayListOf()) }
            val wrapper = event.lastUsedWrapper
            val size = if (wrapper != null) {
                // Rewrite now
                val buffer = wrapper.buffer as ByteBuf
                buffer.clear()
                wrapper.write()
                buffer.writerIndex()
            } else {
                (event.byteBuf as ByteBuf).writerIndex()
            }
            records.add(PacketRecord(size))
        }
    }
}