package io.github.rothes.esu.bukkit.module.optimizations.v1

import io.github.rothes.esu.bukkit.module.optimizations.TicketTypeHandler
import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGet
import io.github.rothes.esu.core.util.UnsafeUtils.usLongAccessor
import net.minecraft.server.level.TicketType
import java.lang.reflect.Modifier

class TicketTypeHandlerImpl: TicketTypeHandler {

    companion object {
        private val expiryTicksField = TicketType::class.java.declaredFields
            .first { it.type == Long::class.java && !Modifier.isStatic(it.modifiers) }
        private val expiryTicksFieldPrivate = Modifier.isPrivate(expiryTicksField.modifiers)
    }

    val map = TicketType::class.java.declaredFields
        .filter { it.type.isAssignableFrom(TicketType::class.java) }
        .map { it.accessibleGet(null) as TicketType<*> }
        .map { getTicketType(it) }
        .associateBy { it.name }

    override fun getTicketTypeMap(): Map<String, TicketTypeHandler.TicketType> {
        return map
    }

    private fun getTicketType(handle: TicketType<*>): TicketTypeHandler.TicketType {
        return if (expiryTicksFieldPrivate) TicketTypeCBImpl(handle) else TicketTypePaperImpl(handle)
    }

    class TicketTypePaperImpl(
        override val handle: TicketType<*>,
    ): TicketTypeHandler.TicketType {

        override val name: String = handle.toString()

        override var expiryTicks: Long
            get() = handle.timeout
            set(value) {
                handle.timeout = value
            }
    }

    // On Spigot, it's not public
    class TicketTypeCBImpl(
        override val handle: TicketType<*>,
    ): TicketTypeHandler.TicketType {

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