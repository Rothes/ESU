package io.github.rothes.esu.velocity.util.extension

import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.velocity.plugin

fun Feature<*, *>.checkPacketEvents(): Feature.AvailableCheck? {
    if (plugin.server.pluginManager.getPlugin("packetevents").isPresent) return null
    plugin.err("[$name] This feature requires packetevents plugin!")
    return Feature.AvailableCheck.fail { "This feature requires packetevents plugin".message }
}