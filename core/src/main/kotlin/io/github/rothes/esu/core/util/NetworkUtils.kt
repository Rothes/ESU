package io.github.rothes.esu.core.util

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URL

object NetworkUtils {

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

    /**************************
        Bandwidth Estimator

        TCP only, no options
    **************************/

    private const val MTU = 1500
    private const val IP_HEADER_SIZE = 20
    private const val TCP_HEADER_SIZE = 20
    private const val L2_OVERHEAD = 12 + 2 + 4 // MAC + Ethertype + FCS
    private const val L1_OVERHEAD = 7 + 1 + 12 // Preamble + SFD + IPG
    private const val FRAME_OVERHEAD = IP_HEADER_SIZE + TCP_HEADER_SIZE
    private const val MIN_PAYLOAD = 46
    private const val MAX_PAYLOAD = MTU - FRAME_OVERHEAD

    fun estimatePackets(size: Int): Int {
        return (size + (MAX_PAYLOAD - 1)) / (MAX_PAYLOAD)
    }

    fun estimateWireFrameBytes(size: Int, options: EstimatorOptions = EstimatorOptions.DEFAULT): Int {
        val frames = (size + (MAX_PAYLOAD - 1)) / (MAX_PAYLOAD)
        val overhead = frames * FRAME_OVERHEAD

        var totalBytes = size + overhead

        if (options.includePadding) {
            val fills = MIN_PAYLOAD - (size - (frames - 1) * MAX_PAYLOAD)
            if (fills > 0) totalBytes += fills
        }
        if (options.includeLayer2) {
            totalBytes += frames * L2_OVERHEAD
        }
        if (options.includeLayer1) {
            totalBytes += frames * L1_OVERHEAD
        }
        return totalBytes
    }

    data class EstimatorOptions(
        val includePadding: Boolean = false,
        val includeLayer2: Boolean = false,
        val includeLayer1: Boolean = false,
    ) {
        companion object {
            val DEFAULT = EstimatorOptions()
        }
    }

}