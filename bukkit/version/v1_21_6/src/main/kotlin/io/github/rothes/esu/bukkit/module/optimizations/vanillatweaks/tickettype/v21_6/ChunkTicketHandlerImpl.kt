package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v21_6

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeCommandsFeature
import io.github.rothes.esu.bukkit.util.CoordinateUtils
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import net.minecraft.server.level.ServerChunkCache
import net.minecraft.world.level.TicketStorage
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld

object ChunkTicketHandlerImpl: TicketTypeCommandsFeature.ChunkTicketHandler {

    private val ACCESSOR = ServerChunkCache::class.java.getDeclaredField("ticketStorage").getter

    override fun sendTicketDebugString(user: User, chunkX: Int, chunkZ: Int, world: World) {
        val chunkSource = (world as CraftWorld).handle.getChunkSource() // This field is private on Spigot, call method getter
        val ticketStorage = ACCESSOR.invokeExact(chunkSource) as TicketStorage
        val chunkKey = CoordinateUtils.getChunkKey(chunkX, chunkZ)
        user.message("Load Ticket: " + ticketStorage.getTicketDebugString(chunkKey, false))
        user.message("Tick Ticket: " + ticketStorage.getTicketDebugString(chunkKey, true))
    }

}