package io.github.rothes.esu.bukkit.module.essentialcommands

import com.google.common.cache.CacheBuilder
import io.github.rothes.esu.bukkit.event.RichPlayerDeathEvent
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.pLang
import io.github.rothes.esu.lib.adventure.text.event.ClickEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import java.time.Duration

object Suicide: BaseCommand<Suicide.Config, Suicide.Lang>() {

    private val suicided = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(1)).build<Player, Unit>()

    override fun onEnable() {
        registerCommands(object {
            @Command("suicide")
            @ShortPerm
            fun suicide(sender: User, @Flag("confirm") confirm: Boolean = false) {
                if (!confirm) {
                    val button = sender.buildMiniMessage(lang, { confirmButton })
                        .clickEvent(ClickEvent.runCommand("/suicide --confirm"))
                    sender.message(lang, { confirmSuicide }, component("confirm", button))
                    return
                }
                val user = sender as PlayerUser
                val player = user.player
                suicided.put(player, Unit)
                Scheduler.schedule(player) {
                    player.health = 0.0
                }
            }
        })
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
    }

    private object Listeners: Listener {

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onDeath(e: RichPlayerDeathEvent) {
            val player = e.player
            suicided.getIfPresent(player) ?: return println("not sui")
            suicided.invalidate(player)
            e.setChatMessage { user, old ->
                old?.let { msg ->
                    user.buildMiniMessage(
                        config.suicideDeathMessage,
                        component("message", msg),
                        pLang(user, lang, { placeholders })
                    )
                }
            }
        }
    }

    data class Config(
        val suicideDeathMessage: String = "<message> <pl:suicided>",
    ): BaseFeatureConfiguration(true)

    data class Lang(
        val placeholders: Map<String, String> = mapOf(
            "suicided" to "(suicided)",
        ),
        val confirmButton: String = "<edc><hover:show_text:'<ec>GoodBye...'>[Confirm]",
        val confirmSuicide: MessageData = "<ec>Are you sure to commit suicide? All your items will be lost! <confirm>".message,
    )

}