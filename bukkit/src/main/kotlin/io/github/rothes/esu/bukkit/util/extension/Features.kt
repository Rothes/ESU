package io.github.rothes.esu.bukkit.util.extension

import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import org.bukkit.Bukkit

fun Feature<*, *>.checkPacketEvents(): Feature.AvailableCheck? {
    if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) return null
    return errFail { "This feature requires packetevents plugin".message }
}