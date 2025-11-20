package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object Invulnerable : PlayerOptionalCommand<Invulnerable.Config, Invulnerable.Lang>() {

    override val aliases: Array<String>
        get() = arrayOf("god")

    private val changed = ConcurrentHashMap.newKeySet<Player>()

    override fun onEnable() {
        super.onEnable()
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
        if (config.autoResetInvulnerable) {
            for (player in changed) {
                player.isInvulnerable = false
            }
        }
        changed.clear()
    }

    override fun onPerform(sender: User, player: Player, silent: Boolean) {
        player.syncTick {
            if (player.isInvulnerable) {
                player.isInvulnerable = false
                sender.message(lang, { disabledForPlayer }, player(player))
                changed.remove(player)
                if (!silent)
                    player.user.message(lang, { disabled })
            } else {
                player.isInvulnerable = true
                sender.message(lang, { enabledForPlayer }, player(player))
                changed.add(player)
                if (!silent)
                    player.user.message(lang, { enabled })
            }
        }
    }

    private object Listeners : Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            if (changed.remove(event.player) && config.autoResetInvulnerable)
                event.player.isInvulnerable = false
        }
    }

    data class Config(
        @Comment("""
            Reset invulnerable for players leaving the server or on disable.
            If set to false, modified invulnerable state may exist forever.
        """)
        val autoResetInvulnerable: Boolean = true,
    ): BaseFeatureConfiguration(true)

    data class Lang(
        val enabledForPlayer: MessageData = "<pc>Enabled invulnerable mode for <pdc><player><pc>.".message,
        val disabledForPlayer: MessageData = "<pc>Disabled invulnerable mode for <pdc><player><pc>.".message,
        val enabled: MessageData = "<pc>You no longer receives any damage.".message,
        val disabled: MessageData = "<pc>You are now receiving damage.".message,
    )
}