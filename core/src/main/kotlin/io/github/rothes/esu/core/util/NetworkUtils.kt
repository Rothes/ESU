package io.github.rothes.esu.core.util

import java.net.InetAddress
import java.net.URI
import java.net.URL

object NetworkUtils {

    val String.uriLatency: Long
        get() = URI.create(this).toURL().latency

    val String.hostLatency: Long
        get() {
            val address = InetAddress.getByName(this)
            val start = System.currentTimeMillis()
            val reachable = address.isReachable(500)
            if (!reachable)
                return -1
            val latency = System.currentTimeMillis() - start
            return latency
        }

    val URL.latency: Long
        get() = host.hostLatency

}