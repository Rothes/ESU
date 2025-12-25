package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v21_6

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeAdvancedHandler
import io.github.rothes.esu.core.util.UnsafeUtils.usBooleanAccessor
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.server.level.TicketType

object TicketTypeAdvancedHandlerImpl : TicketTypeAdvancedHandler {

    private val PERSIST_ACCESSOR = TicketType::class.java.getDeclaredField("persist").usBooleanAccessor
    private val USE_ACCESSOR = TicketType::class.java.getDeclaredField("use").usObjAccessor

    override var TicketType<*>.persist: Boolean
        get() = persist()
        set(value) {
            PERSIST_ACCESSOR[this] = value
        }

    override var TicketType<*>.loadsChunk: Boolean
        get() = doesLoad()
        set(value) {
            USE_ACCESSOR[this] =
                if (value) {
                    if (doesSimulate()) TicketType.TicketUse.LOADING_AND_SIMULATION else TicketType.TicketUse.LOADING
                } else {
                    // It's not possible to no load and tick at the same time.
                    // We reverse to the other one, we set loadsChunk first and ticksChunk later,
                    // we don't want it breaks the later ticksChunk setting.
                    TicketType.TicketUse.SIMULATION
                }
        }

    override var TicketType<*>.ticksChunk: Boolean
        get() = doesSimulate()
        set(value) {
            USE_ACCESSOR[this] =
                if (value) {
                    if (doesLoad()) TicketType.TicketUse.LOADING_AND_SIMULATION else TicketType.TicketUse.SIMULATION
                } else {
                    TicketType.TicketUse.LOADING
                }
        }

    override var TicketType<*>.keepsDimensionActive: Boolean
        get() = error("Server Unsupported")
        set(_) { error("Server Unsupported") }
    override var TicketType<*>.expiresIfUnloaded: Boolean
        get() = error("Server Unsupported")
        set(_) { error("Server Unsupported") }


}