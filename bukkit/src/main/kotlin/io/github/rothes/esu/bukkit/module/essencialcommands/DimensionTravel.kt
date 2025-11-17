package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.bukkit.util.WorldUtils
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.extension.math.floorI
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command

object DimensionTravel : BaseCommand<FeatureToggle.DefaultTrue, DimensionTravel.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("dimensionTravel")
            @ShortPerm
            suspend fun dimensionTravel(sender: User) {
                val user = sender as PlayerUser
                dimensionTravel(sender, user.player)
            }

            @Command("dimensionTravel <player>")
            @ShortPerm("others")
            suspend fun dimensionTravel(sender: User, player: Player) {
                val location = player.location
                val world = location.world
                val target = when (world.environment) {
                    World.Environment.NORMAL -> {
                        val world = Bukkit.getWorld("world_nether")
                        val x = location.x / 8
                        val z = location.z / 8
                        Location(world, x, 0.0, z)
                    }
                    World.Environment.NETHER -> {
                        val world = Bukkit.getWorld("world")
                        val x = location.x.floorI() shl 3
                        val z = location.z.floorI() shl 3
                        Location(world, x.toDouble(), 0.0, z.toDouble())
                    }
                    World.Environment.THE_END -> {
                        sender.message(lang, { unsupportedTheEnd })
                        return
                    }
                    else -> error("Unsupported world environment ${world.environment}")
                }
                target.yaw = location.yaw
                target.pitch = location.pitch
                val safeSpot = WorldUtils.findSafeSpot(target) ?: return sender.message(module.lang, { unsafeTeleportSpot })
                player.tp(safeSpot)
                sender.message(module.lang, { teleportingPlayer }, player(player))
            }
        })
    }

    data class Lang(
        val unsupportedTheEnd: MessageData = "<ec>Dimension travel is not possible from the end.".message,
    )
}