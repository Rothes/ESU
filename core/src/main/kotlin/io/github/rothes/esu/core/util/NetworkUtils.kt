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

}