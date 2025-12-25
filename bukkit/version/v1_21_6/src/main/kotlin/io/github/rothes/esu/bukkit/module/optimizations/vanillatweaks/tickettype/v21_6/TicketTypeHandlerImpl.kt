package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v21_6

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeHandler
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import io.github.rothes.esu.core.util.UnsafeUtils.usLongAccessor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.TicketType

object TicketTypeHandlerImpl: TicketTypeHandler {

    private val KEY_HANDLER by Versioned(ResourceKeyHandler::class.java)

    val map = BuiltInRegistries.TICKET_TYPE
        .entrySet()
        .associate {
            val name = KEY_HANDLER.getResourceKeyString(it.key)
            name to wrapTicketType(it.value, name)
        }

    override fun getTicketTypeMap(): Map<String, TicketTypeHandler.NmsTicketType> {
        return map
    }

    private fun wrapTicketType(handle: TicketType<*>, name: String): TicketTypeHandler.NmsTicketType {
        return if (ServerCompatibility.isPaper) NmsTicketTypeMoonriseImpl(handle, name) else NmsTicketTypeCBImpl(handle, name)
    }

    class NmsTicketTypeMoonriseImpl(
        override val handle: TicketType<*>,
        override val name: String,
    ): TicketTypeHandler.NmsTicketType {

        override var expiryTicks: Long
            get() = handle.timeout()
            set(value) {
                handle.`moonrise$setTimeout`(value)
            }
    }

    class NmsTicketTypeCBImpl(
        override val handle: TicketType<*>,
        override val name: String,
    ): TicketTypeHandler.NmsTicketType {

        override var expiryTicks: Long
            get() = handle.timeout()
            set(value) {
                TIMEOUT[handle] = value
            }

        companion object {
            private val TIMEOUT = TicketType::class.java.getDeclaredField("timeout").usLongAccessor
        }
    }

}