package io.github.rothes.esu.bukkit.util.extension

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.Feature
import org.bukkit.Bukkit

fun Feature<*, *>.checkPacketEvents(): Feature.AvailableCheck? {
    if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) return null
    plugin.err("[$name] This feature requires packetevents plugin!")
    return Feature.AvailableCheck.fail { "This feature requires packetevents plugin".message }
}