package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v1

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeCommandsFeature
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.CoordinateUtils
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import io.github.rothes.esu.core.util.extension.math.floorI
import net.minecraft.server.level.DistanceManager
import net.minecraft.server.level.ServerChunkCache
import org.bukkit.World
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld
import org.incendo.cloud.annotations.Command

object TicketTypeCommandsObjImpl: TicketTypeCommandsFeature.TicketTypeCommandsObj {

    private val ACCESSOR = ServerChunkCache::class.java.getDeclaredField("distanceManager").getter
    private val INVOKER = DistanceManager::class.java.getDeclaredMethod("getTicketDebugString", Long::class.java).handle

    @Command("esu optimizations ticketType getTickets [chunkX] [chunkZ] [world]")
    @ShortPerm
    fun getTickets(sender: PlayerUser,
                   chunkX: Int = sender.player.location.x.floorI() shr 4, chunkZ: Int = sender.player.location.z.floorI() shr 4,
                   world: World = sender.player.world) {
        val chunkSource = (world as CraftWorld).handle.getChunkSource() // This field is private on Spigot, call method getter
        val distanceManager = ACCESSOR.invokeExact(chunkSource) as DistanceManager
        val chunkKey = CoordinateUtils.getChunkKey(chunkX, chunkZ)
        sender.message(INVOKER.invokeExact(distanceManager, chunkKey) as String)
    }

}