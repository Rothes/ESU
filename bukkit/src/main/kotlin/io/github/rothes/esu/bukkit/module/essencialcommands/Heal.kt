package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag

object Heal : CommonFeature<FeatureToggle.DefaultTrue, Heal.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("heal")
            @ShortPerm
            fun heal(sender: User) {
                val user = sender as PlayerUser
                heal(sender, user.player, true)
            }

            @Command("heal <player>")
            @ShortPerm("others")
            fun heal(sender: User, player: Player, @Flag("silent") silent: Boolean = sender.uuid != player.uniqueId) {
                player.heal(player.getAttribute(AttributeAdapter.MAX_HEALTH)!!.value)
                sender.message(lang, { healedPlayer }, player(player))
                if (!silent) {
                    player.user.message(lang, { healed })
                }
            }
        })
    }

    data class Lang(
        val healed: MessageData = "<pc>You have been healed.".message,
        val healedPlayer: MessageData = "<pc>Healed <pdc><player></pc>.".message,
    )
}