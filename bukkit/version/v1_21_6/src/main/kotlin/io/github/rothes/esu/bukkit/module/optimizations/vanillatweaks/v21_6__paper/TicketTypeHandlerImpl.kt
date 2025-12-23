package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.v21_6__paper

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.TicketTypeFeature
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.TicketType

object TicketTypeHandlerImpl: TicketTypeFeature.TicketTypeHandler {

    private val KEY_HANDLER by Versioned(ResourceKeyHandler::class.java)

    val map = BuiltInRegistries.TICKET_TYPE
        .entrySet()
        .associate {
            val name = KEY_HANDLER.getResourceKeyString(it.key)
            name to TicketTypeMoonriseImpl(it.value, name)
        }

    override fun getTicketTypeMap(): Map<String, TicketTypeFeature.TicketTypeHandler.TicketType> {
        return map
    }

    class TicketTypeMoonriseImpl(
        override val handle: TicketType<*>,
        override val name: String,
    ): TicketTypeFeature.TicketTypeHandler.TicketType {

        override var expiryTicks: Long
            get() = handle.timeout()
            set(value) {
                handle.`moonrise$setTimeout`(value)
            }
    }

}