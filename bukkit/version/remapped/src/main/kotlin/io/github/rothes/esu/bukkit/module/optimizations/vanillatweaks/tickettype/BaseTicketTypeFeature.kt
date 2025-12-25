package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.module.CommonFeature

abstract class BaseTicketTypeFeature<C, L> : CommonFeature<C, L>() {

    private var previousSettingHash: Int = 0

    val advancedHandler
        get() = TicketTypeAdvancedHandler::class.java.versioned()

    override fun onEnable() {
        applySettings()
    }

    override fun onReload() {
        super.onReload()
        if (enabled)
            applySettings()
    }

    abstract fun apply()

    private fun applySettings() {
        val config = config
        if (previousSettingHash == config.hashCode()) return
        apply()
        previousSettingHash = config.hashCode()
    }

    protected fun findTicketType(key: String): TicketTypeHandler.NmsTicketType? {
        val ticketType = TicketTypeHandler.handler.getTicketTypeMap()[key]
        if (ticketType == null) {
            core.err("[$name] Ticket type $key does not exists!")
            return null
        }
        return ticketType
    }

}