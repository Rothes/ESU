package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.entity.Player

object Ping : PlayerOptionalCommand<FeatureToggle.DefaultTrue, Ping.Lang>() {

    override val receivesSilentFlag: Boolean
        get() = false

    override fun onPerform(sender: User, player: Player, silent: Boolean) {
        sender.message(lang, { message }, player(player), unparsed("ping", player.ping))
    }

    data class Lang(
        val message: MessageData = "<pdc><player><pc>'s ping is <sdc><ping><sc>ms".message,
    )

}