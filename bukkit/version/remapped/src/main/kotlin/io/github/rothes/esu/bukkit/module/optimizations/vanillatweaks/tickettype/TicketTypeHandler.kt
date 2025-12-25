package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.util.version.Versioned
import net.minecraft.server.level.TicketType

interface TicketTypeHandler {

    fun getTicketTypeMap(): Map<String, NmsTicketType>

    interface NmsTicketType {
        val handle: TicketType<*>
        val name: String
        var expiryTicks: Long
    }

    companion object {
        val handler by Versioned(TicketTypeHandler::class.java)
    }
}