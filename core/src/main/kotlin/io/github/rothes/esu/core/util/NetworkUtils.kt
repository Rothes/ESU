package io.github.rothes.esu.core.util

import io.github.rothes.esu.core.EsuCore
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.net.URL

object NetworkUtils {

    val String.uriLatency: Long
        get() = URI.create(this).toURL().latency

    val String.hostLatency: Long
        get() {
            try {
                val address = InetAddress.getByName(this)
                val start = System.currentTimeMillis()
                val reachable = address.isReachable(500)
                if (!reachable) return -1
                val latency = System.currentTimeMillis() - start
                return latency
            } catch (e: IOException) {
                EsuCore.instance.err("Failed to test latency to $this : $e")
                return -1
            }
        }

    val URL.latency: Long
        get() = host.hostLatency

}