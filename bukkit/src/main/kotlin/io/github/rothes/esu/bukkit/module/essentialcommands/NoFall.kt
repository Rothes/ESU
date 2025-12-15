package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object NoFall : PlayerOptionalCommand<FeatureToggle.DefaultTrue, NoFall.Lang>() {

    private val players = ConcurrentHashMap.newKeySet<Player>()

    override fun onEnable() {
        super.onEnable()
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
        players.clear()
    }

    override fun onPerform(sender: User, player: Player, silent: Boolean) {
        if (players.add(player)) {
            sender.message(lang, { enabledForPlayer }, player(player))
            if (!silent)
                player.user.message(lang, { enabled })
        } else {
            players.remove(player)
            sender.message(lang, { disabledForPlayer }, player(player))
            if (!silent)
                player.user.message(lang, { disabled })
        }
    }

    private object Listeners: Listener {
        @EventHandler
        fun onFallDamage(e: EntityDamageEvent) {
            if (e.cause == EntityDamageEvent.DamageCause.FALL && players.contains(e.entity)) {
                e.isCancelled = true
            }
        }

        @EventHandler
        fun onQuit(e: PlayerQuitEvent) {
            players.remove(e.player)
        }
    }

    data class Lang(
        val enabledForPlayer: MessageData = "<pc>Enabled no fall for <pdc><player><pc>.".message,
        val disabledForPlayer: MessageData = "<pc>Disabled no fall for <pdc><player><pc>.".message,
        val enabled: MessageData = "<pc>You no longer receives fall damage.".message,
        val disabled: MessageData = "<pc>You are now receiving fall damage.".message,
    )

}