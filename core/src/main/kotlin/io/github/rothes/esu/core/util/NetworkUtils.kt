package io.github.rothes.esu.core.util

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URL

object NetworkUtils {

    private const val MIN_PAYLOAD = 46
    private const val MAX_PAYLOAD = 1500
    private const val ETHERNET_FRAME_OVERHEAD = 6 + 6 + 2 + 4 // MAC destination + MAC source + EtherType/length + CRC

    val String.uriLatency: Long
        get() = URI.create(this).toURL().latency

    val String.hostLatency: Long
        get() {
            // Use Socket instead of InetAddress.isReachable (which requires root on linux) for platform independent
            val socket = Socket()
            try {
                val start = System.currentTimeMillis()
                socket.connect(InetSocketAddress(this, 80), 500)
                val latency = System.currentTimeMillis() - start
                return latency
            } catch (_: IOException) {
                return -1
            } finally {
                socket.close() // No kotlin-stdlib
            }
        }

    val URL.latency: Long
        get() = host.hostLatency

    fun calculateEthernetFrameBytes(size: Int): Int {
        val frames = (size + (MAX_PAYLOAD - 1)) / (MAX_PAYLOAD)
        val overhead = frames * ETHERNET_FRAME_OVERHEAD
        val fills = MIN_PAYLOAD - (size - (frames - 1) * MAX_PAYLOAD)

        var ethernetFrameBytes = size + overhead
        if (fills > 0) ethernetFrameBytes += fills
        return ethernetFrameBytes
    }

}