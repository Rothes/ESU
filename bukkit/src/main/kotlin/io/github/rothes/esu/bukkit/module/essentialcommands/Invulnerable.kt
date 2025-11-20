package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player

object Invulnerable : PlayerOptionalCommand<FeatureToggle.DefaultTrue, Invulnerable.Lang>() {

    override val aliases: Array<String>
        get() = arrayOf("god")

    override fun onPerform(sender: User, player: Player, silent: Boolean) {
        player.syncTick {
            if (player.isInvulnerable) {
                player.isInvulnerable = false
                sender.message(lang, { disabledForPlayer }, player(player))
                if (!silent)
                    player.user.message(lang, { disabled })
            } else {
                player.isInvulnerable = true
                sender.message(lang, { enabledForPlayer }, player(player))
                if (!silent)
                    player.user.message(lang, { enabled })
            }
        }
    }

    data class Lang(
        val enabledForPlayer: MessageData = "<pc>Enabled invulnerable mode for <pdc><player><pc>.".message,
        val disabledForPlayer: MessageData = "<pc>Disabled invulnerable mode for <pdc><player><pc>.".message,
        val enabled: MessageData = "<pc>You no longer receives any damage.".message,
        val disabled: MessageData = "<pc>You are now receiving damage.".message,
    )
}