package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.user
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.incendo.cloud.annotations.Command

object ClientLocale : BaseCommand<FeatureToggle.DefaultTrue, ClientLocale.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("clientLocale [player]")
            @ShortPerm
            fun clientLocale(sender: User, player: User = sender) {
                sender.message(lang, { message }, user(player), unparsed("locale", player.clientLocale))
            }
        })
    }

    data class Lang(
        val message: MessageData = "<pdc><player><pc>'s client locale is <sdc><locale>".message,
    )

}