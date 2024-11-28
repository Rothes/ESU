package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.serializer.OptionalSerializer
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.spongepowered.configurate.objectmapping.meta.Comment

object BetterEventMessagesModule: BukkitModule<BetterEventMessagesModule.ModuleConfig, EmptyConfiguration>(
    ModuleConfig::class.java, EmptyConfiguration::class.java
) {

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
    }

    override fun disable() {
        super.disable()
        HandlerList.unregisterAll(Listeners)
    }

    object Listeners: Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onDeath(event: PlayerDeathEvent) {
            val message = event.deathMessage() ?: return
            event.deathMessage(handle(message, config.message.death))
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onJoin(event: PlayerJoinEvent) {
            val message = event.joinMessage() ?: return
            event.joinMessage(handle(message, config.message.playerJoin))
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onQuit(event: PlayerQuitEvent) {
            val message = event.quitMessage() ?: return
            event.quitMessage(handle(message, config.message.playerQuit))
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onQuit(event: PlayerAdvancementDoneEvent) {
            val message = event.message() ?: return
            event.message(handle(message, config.message.doneAdvancement))
        }

        private fun handle(message: Component, modifier: ModuleConfig.Message.MessageModifier): Component? {
            val handler = { user: User ->
                user.message(
                    Component.text()
                        .append(user.buildMinimessage(modifier.head))
                        .append(modifier.color?.let { message.color(it) } ?: message)
                        .append(user.buildMinimessage(modifier.foot))
                        .build()
                )
            }
            Bukkit.getOnlinePlayers().forEach {
                handler(it.user)
            }
            if (modifier.showInConsole) {
                handler(ConsoleUser)
            }
            return null
        }

    }

    data class ModuleConfig(
        @field:Comment("""
Customize the message behaviours.
Set 'color' to '${OptionalSerializer.DISABLED}' or a color to change the default color of the message.
If 'show-in-console' is false, only the online players can see the message.""")
        val message: Message = Message(),
    ): BaseModuleConfiguration() {

        data class Message(
            val death: MessageModifier = MessageModifier(TextColor.fromHexString("#dd9090")),
            val doneAdvancement: MessageModifier = MessageModifier(TextColor.fromHexString("#edde0e")),
            val playerJoin: MessageModifier = MessageModifier(NamedTextColor.GRAY, "<pc>[<pdc>+<pc>] "),
            val playerQuit: MessageModifier = MessageModifier(NamedTextColor.GRAY, "<pc>[<pdc>-<pc>] "),
        ): ConfigurationPart {

            data class MessageModifier(
                val color: TextColor? = null,
                val head: String = "",
                val foot: String = "",
                val showInConsole: Boolean = true
            ): ConfigurationPart

        }

    }

}