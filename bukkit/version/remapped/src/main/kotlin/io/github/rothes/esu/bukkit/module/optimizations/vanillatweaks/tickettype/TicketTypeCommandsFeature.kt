package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.extension.math.floorI
import org.bukkit.World
import org.incendo.cloud.annotations.Command

object TicketTypeCommandsFeature: CommonFeature<FeatureToggle.DefaultFalse, Unit>() {

    override fun onEnable() {
        val handler by Versioned(ChunkTicketHandler::class.java)

        registerCommands(object {
            @Command("esu optimizations ticketType getTickets [chunkX] [chunkZ] [world]")
            @ShortPerm
            fun getTickets(sender: User,
                           chunkX: Int = floorI(sender.player.x) shr 4, chunkZ: Int = floorI(sender.player.z) shr 4,
                           world: World = sender.player.world) {
                handler.sendTicketDebugString(sender, chunkX, chunkZ, world)
            }

            private val User.player
                get() = (this as PlayerUser).player
        })
    }

    interface ChunkTicketHandler {

        fun sendTicketDebugString(user: User, chunkX: Int, chunkZ: Int, world: World)

    }

}
