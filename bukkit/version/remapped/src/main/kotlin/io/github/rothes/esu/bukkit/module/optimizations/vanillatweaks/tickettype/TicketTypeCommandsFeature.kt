package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle

object TicketTypeCommandsFeature: CommonFeature<FeatureToggle.DefaultFalse, Unit>() {

    override fun onEnable() {
        registerCommands(TicketTypeCommandsObj::class.java.versioned()) { parser ->
            parser.registerBuilderDecorator {
                it.senderType(PlayerUser::class.java)
            }
        }
    }

    interface TicketTypeCommandsObj

}