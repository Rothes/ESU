package io.github.rothes.esu.bukkit.module.optimizations.v1

import io.github.rothes.esu.bukkit.module.optimizations.TicketTypeHandler
import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGet
import net.minecraft.server.level.TicketType

class TicketTypeHandlerImpl: TicketTypeHandler {

    val map = TicketType::class.java.declaredFields
        .filter { it.type.isAssignableFrom(TicketType::class.java) }
        .map { it.accessibleGet(null) as TicketType<*> }
        .map { TicketTypeImpl(it) }
        .associateBy { it.name }

    override fun getTicketTypeMap(): Map<String, TicketTypeHandler.TicketType> {
        return map
    }

    class TicketTypeImpl(
        override val handle: TicketType<*>,
    ): TicketTypeHandler.TicketType {

        override val name: String = handle.toString()

        override var expiryTicks: Long
            get() = handle.timeout
            set(value) {
                handle.timeout = value
            }
    }
}