package io.github.rothes.esu.bukkit.module.optimizations.v21_6__paper

import io.github.rothes.esu.bukkit.module.optimizations.TicketTypeHandler
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.TicketType

class TicketTypeHandlerImpl: TicketTypeHandler {

    val map = BuiltInRegistries.TICKET_TYPE
        .entrySet()
        .associate {
            val name = it.key.location().path
            name to TicketTypeMoonriseImpl(it.value, name)
        }

    override fun getTicketTypeMap(): Map<String, TicketTypeHandler.TicketType> {
        return map
    }

    class TicketTypeMoonriseImpl(
        override val handle: TicketType<*>,
        override val name: String,
    ): TicketTypeHandler.TicketType {

        override var expiryTicks: Long
            get() = handle.timeout()
            set(value) {
                handle.`moonrise$setTimeout`(value)
            }
    }

}