package io.github.rothes.esu.bukkit.module.optimizations

import io.github.rothes.esu.bukkit.util.version.Versioned

interface TicketTypeHandler {

    fun getTicketTypeMap(): Map<String, TicketType>

    interface TicketType {
        val handle: Any
        val name: String
        var expiryTicks: Long
    }

    companion object {
        val handler by Versioned(TicketTypeHandler::class.java)
    }
}