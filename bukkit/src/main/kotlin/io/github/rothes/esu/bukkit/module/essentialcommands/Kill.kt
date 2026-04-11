package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command

object Kill : BaseCommand<FeatureToggle.DefaultTrue, Kill.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("kill <player>")
            @ShortPerm("others")
            fun kill(sender: User, player: Player) {
                player.syncTick {
                    player.health = 0.0
                    sender.message(lang, { killedPlayer }, player(player))
                }
            }
        })
    }

    data class Lang(
        val killedPlayer: MessageData = "<pc>Killed player <pdc><player></pdc> .".message,
    )

}