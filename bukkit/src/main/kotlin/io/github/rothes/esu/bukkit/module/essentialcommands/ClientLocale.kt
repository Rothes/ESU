package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.entity.Player

object ClientLocale : PlayerOptionalCommand<FeatureToggle.DefaultTrue, ClientLocale.Lang>() {

    override val receivesSilentFlag: Boolean
        get() = false

    override fun onPerform(sender: User, player: Player, silent: Boolean) {
        val playerUser = player.user
        sender.message(lang, { message }, player(player), unparsed("locale", playerUser.clientLocale))
    }

    data class Lang(
        val message: MessageData = "<pdc><player><pc>'s client locale is <sdc><locale>".message,
    )

}