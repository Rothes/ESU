package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.bukkit.command.parser.location.ChunkLocation
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command

object TpChunk : CommonFeature<FeatureToggle.DefaultTrue, TpChunk.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("tpChunk <chunk> [world]")
            @ShortPerm
            fun tpChunk(sender: User, chunk: ChunkLocation, world: World = (sender as PlayerUser).player.location.world) {
                tpChunk(sender, chunk, world, (sender as PlayerUser).player)
            }
            @Command("tpChunk <chunk> <world> <player>")
            @ShortPerm("others")
            fun tpChunk(sender: User, chunk: ChunkLocation, world: World, player: Player) {
                sender.message(lang, { teleporting }, player(player))
                val location = Location(world, (chunk.chunkX shl 4) + 8.5, 0.0, (chunk.chunkZ shl 4) + 8.5)
                Scheduler.schedule(location) {
                    val y = if (world.environment == World.Environment.NETHER) {
                        var y = 125
                        while (y > 0) {
                            y--
                            if (world.getBlockAt(location.blockX, y + 2, location.blockZ).type.isAir
                                && world.getBlockAt(location.blockX, y + 1, location.blockZ).type.isAir
                                && world.getBlockAt(location.blockX, y, location.blockZ).type.isSolid) {
                                break
                            }
                        }
                        y
                    } else {
                        world.getHighestBlockYAt(location)
                    }
                    location.y = y.toDouble() + 1
                    player.tp(location)
                }
            }
        })
    }

    data class Lang(
        val teleporting: MessageData = "<tc>Teleporting <tdc><player></tdc>...".message,
    )

}