package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command

object Ping : BaseCommand<FeatureToggle.DefaultTrue, Ping.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("ping [player]")
            @ShortPerm
            fun ping(sender: User, player: Player = (sender as PlayerUser).player) {
                sender.message(lang, { message }, player(player), unparsed("ping", player.ping))
            }
        })
    }

    data class Lang(
        val message: MessageData = "<pdc><player><pc>'s ping is <sdc><ping><sc>ms".message,
    )

}