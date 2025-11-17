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
import org.bukkit.World
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag

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
            suspend fun dimensionTravel(sender: User, player: Player, @Flag("unsafe") unsafe: Boolean = false) {
                val target = player.location
                val world = target.world
                when (world.environment) {
                    World.Environment.NORMAL -> {
                        target.world = Bukkit.getWorld("world_nether")
                        target.x /= 8
                        target.z /= 8
                    }
                    World.Environment.NETHER -> {
                        target.world = Bukkit.getWorld("world")
                        target.x = (target.x.floorI() shl 3).toDouble()
                        target.z = (target.z.floorI() shl 3).toDouble()
                    }
                    World.Environment.THE_END -> {
                        sender.message(lang, { unsupportedTheEnd })
                        return
                    }
                    else -> error("Unsupported world environment ${world.environment}")
                }
                val spot = WorldUtils.findStandableSpot(target, unsafe) ?: return sender.message(module.lang, { unsafeTeleportSpot })
                player.tp(spot)
                sender.message(module.lang, { teleportingPlayer }, player(player))
            }
        })
    }

    data class Lang(
        val unsupportedTheEnd: MessageData = "<ec>Dimension travel is not possible from the end.".message,
    )
}