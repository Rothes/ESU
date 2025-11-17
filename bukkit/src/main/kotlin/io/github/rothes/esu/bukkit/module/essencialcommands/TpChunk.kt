package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.bukkit.command.parser.location.ChunkLocation
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.bukkit.util.WorldUtils
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag

object TpChunk : BaseCommand<FeatureToggle.DefaultTrue, Unit>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("tpChunk <chunk> [world]")
            @ShortPerm
            suspend fun tpChunk(sender: User, chunk: ChunkLocation, world: World = (sender as PlayerUser).player.location.world) {
                tpChunk(sender, chunk, world, (sender as PlayerUser).player)
            }
            @Command("tpChunk <chunk> [world] [player]")
            @ShortPerm("others")
            suspend fun tpChunk(
                sender: User, chunk: ChunkLocation, world: World = (sender as PlayerUser).player.location.world,
                player: Player = (sender as PlayerUser).player,
                @Flag("unsafe") unsafe: Boolean = false
            ) {
                val location = player.location
                val target = Location(world, (chunk.chunkX shl 4) + 8.0, location.y, (chunk.chunkZ shl 4) + 8.0, location.yaw, location.pitch)
                val spot = WorldUtils.findStandableSpot(target, unsafe) ?: return sender.message(module.lang, { unsafeTeleportSpot })
                player.tp(spot)
                sender.message(module.lang, { teleportingPlayer }, player(player))
            }
        })
    }

}