package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.v1.tickettype

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeHandler
import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGet
import io.github.rothes.esu.core.util.UnsafeUtils.usLongAccessor
import net.minecraft.server.level.TicketType
import java.lang.reflect.Modifier

object TicketTypeHandlerImpl: TicketTypeHandler {

    private val expiryTicksField = TicketType::class.java.declaredFields
        .first { it.type == Long::class.java && !Modifier.isStatic(it.modifiers) }
    private val expiryTicksFieldPrivate = Modifier.isPrivate(expiryTicksField.modifiers)

    val map = TicketType::class.java.declaredFields
        .filter { it.type.isAssignableFrom(TicketType::class.java) }
        .map { it.accessibleGet(null) as TicketType<*> }
        .map { wrapTicketType(it) }
        .associateBy { it.name }

    override fun getTicketTypeMap(): Map<String, TicketTypeHandler.NmsTicketType> {
        return map
    }

    private fun wrapTicketType(handle: TicketType<*>): TicketTypeHandler.NmsTicketType {
        return if (expiryTicksFieldPrivate) NmsTicketTypeCBImpl(handle) else NmsTicketTypePaperImpl(handle)
    }

    class NmsTicketTypePaperImpl(
        override val handle: TicketType<*>,
    ): TicketTypeHandler.NmsTicketType {

        override val name: String = handle.toString()

        override var expiryTicks: Long
            get() = handle.timeout
            set(value) {
                handle.timeout = value
            }
    }

    // On Spigot, it's not public
    class NmsTicketTypeCBImpl(
        override val handle: TicketType<*>,
    ): TicketTypeHandler.NmsTicketType {

        override val name: String = handle.toString()

        override var expiryTicks: Long
            get() = accessor[handle]
            set(value) {
                accessor[handle] = value
            }

        companion object {
            private val accessor = expiryTicksField.usLongAccessor
        }
    }

}