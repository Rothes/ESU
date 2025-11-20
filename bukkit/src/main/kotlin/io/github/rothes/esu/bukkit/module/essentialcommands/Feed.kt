package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player

object Feed : PlayerOptionalCommand<FeatureToggle.DefaultTrue, Feed.Lang>() {

    override fun onPerform(sender: User, player: Player, silent: Boolean) {
        player.syncTick {
            player.foodLevel = 20
            player.saturation = 20.0f // 5.0f on respawn
            player.exhaustion = 0.0f
            sender.message(lang, { fedPlayer }, player(player))
            if (!silent) {
                player.user.message(lang, { fed })
            }
        }
    }

    data class Lang(
        val fed: MessageData = "<pc>You have been fed.".message,
        val fedPlayer: MessageData = "<pc>Fed <pdc><player></pc>.".message,
    )
}