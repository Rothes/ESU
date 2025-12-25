package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.CoordinateUtils
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import io.github.rothes.esu.core.util.extension.math.floorI
import net.minecraft.server.level.ServerChunkCache
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.TicketStorage
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import org.incendo.cloud.annotations.Command

object TicketTypeCommandsFeature: CommonFeature<FeatureToggle.DefaultFalse, Unit>() {

    override fun onEnable() {
        registerCommands(object {
            private val ACCESSOR = ServerChunkCache::class.java.getDeclaredField("ticketStorage").getter

            @Command("esu optimizations ticketType getTickets [chunkX] [chunkZ] [world]")
            @ShortPerm
            fun getTickets(sender: PlayerUser,
                           chunkX: Int = sender.player.x.floorI() shr 4, chunkZ: Int = sender.player.z.floorI() shr 4,
                           world: World = sender.player.world) {
                val level = (world as CraftWorld).handle as LevelAccessor // Convert to LevelAccessor
                val chunkSource = level.chunkSource  // This field is private on Spigot, call method getter
                val ticketStorage = ACCESSOR.invoke(chunkSource) as TicketStorage
                val chunkKey = CoordinateUtils.getChunkKey(chunkX, chunkZ)
                sender.message("Load Ticket: " + ticketStorage.getTicketDebugString(chunkKey, false))
                sender.message("Tick Ticket: " + ticketStorage.getTicketDebugString(chunkKey, true))
            }
        }) { parser ->
            parser.registerBuilderDecorator {
                it.senderType(PlayerUser::class.java)
            }
        }
    }

}