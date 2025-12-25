package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v21_9

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeAdvancedHandler
import io.github.rothes.esu.core.util.UnsafeUtils.usIntAccessor
import net.minecraft.server.level.TicketType

object TicketTypeAdvancedHandlerImpl : TicketTypeAdvancedHandler {

    private val FLAG_ACCESSOR = TicketType::class.java.getDeclaredField("flags").usIntAccessor

    override var TicketType<*>.persist: Boolean
        get() = persist()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111111110
            FLAG_ACCESSOR[this] = if (value) current or 0b1 else current
        }
    override var TicketType<*>.loadsChunk: Boolean
        get() = doesLoad()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111111101
            FLAG_ACCESSOR[this] = if (value) current or 0b10 else current
        }
    override var TicketType<*>.ticksChunk: Boolean
        get() = doesSimulate()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111111011
            FLAG_ACCESSOR[this] = if (value) current or 0b100 else current
        }
    override var TicketType<*>.keepsDimensionActive: Boolean
        get() = shouldKeepDimensionActive()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111110111
            FLAG_ACCESSOR[this] = if (value) current or 0b1000 else current
        }
    override var TicketType<*>.expiresIfUnloaded: Boolean
        get() = canExpireIfUnloaded()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111101111
            FLAG_ACCESSOR[this] = if (value) current or 0b10000 else current
        }

}