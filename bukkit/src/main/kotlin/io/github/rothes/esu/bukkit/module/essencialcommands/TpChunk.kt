package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.bukkit.command.parser.location.ChunkLocation
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.bukkit.util.WorldUtils
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
            suspend fun tpChunk(sender: User, chunk: ChunkLocation, world: World = (sender as PlayerUser).player.location.world) {
                tpChunk(sender, chunk, world, (sender as PlayerUser).player)
            }
            @Command("tpChunk <chunk> [world] [player]")
            @ShortPerm("others")
            suspend fun tpChunk(sender: User, chunk: ChunkLocation, world: World = (sender as PlayerUser).player.location.world, player: Player = (sender as PlayerUser).player) {
                sender.message(lang, { teleporting }, player(player))
                val location = Location(world, (chunk.chunkX shl 4) + 8.0, 0.0, (chunk.chunkZ shl 4) + 8.0)
                val safeSpot = WorldUtils.findSafeSpot(location) ?: return sender.message(lang, { unsafeSpot })
                player.tp(safeSpot)
            }
        })
    }

    data class Lang(
        val unsafeSpot: MessageData = "<ec>Cannot find a safe spot for teleport.".message,
        val teleporting: MessageData = "<tc>Teleporting <tdc><player></tdc>...".message,
    )

}