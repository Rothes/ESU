package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import net.minecraft.server.level.TicketType

interface TicketTypeAdvancedHandler {

    var TicketType<*>.persist: Boolean
    var TicketType<*>.loadsChunk: Boolean
    var TicketType<*>.ticksChunk: Boolean
    var TicketType<*>.keepsDimensionActive: Boolean
    var TicketType<*>.expiresIfUnloaded: Boolean

}