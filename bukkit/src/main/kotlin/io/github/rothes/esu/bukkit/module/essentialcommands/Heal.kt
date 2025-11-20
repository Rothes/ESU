package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player

object Heal : PlayerOptionalCommand<FeatureToggle.DefaultTrue, Heal.Lang>() {

    override fun onPerform(sender: User, player: Player, silent: Boolean) {
        player.syncTick {
            player.heal(player.getAttribute(AttributeAdapter.MAX_HEALTH)!!.value)
            sender.message(lang, { healedPlayer }, player(player))
            if (!silent) {
                player.user.message(lang, { healed })
            }
        }
    }

    data class Lang(
        val healed: MessageData = "<pc>You have been healed.".message,
        val healedPlayer: MessageData = "<pc>Healed <pdc><player></pc>.".message,
    )
}