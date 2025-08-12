package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.RemovedNode
import io.github.rothes.esu.core.configuration.meta.RenamedFrom
import io.github.rothes.esu.core.configuration.serializer.OptionalSerializer
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.OptionalUtils.applyTo
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import io.github.rothes.esu.lib.net.kyori.adventure.text.format.NamedTextColor
import io.github.rothes.esu.lib.net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

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
            event.deathMessage(handle(message.esu, config.message.death)?.server)
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onJoin(event: PlayerJoinEvent) {
            val message = event.joinMessage() ?: return
            event.joinMessage(handle(message.esu, config.message.playerJoin)?.server)
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onQuit(event: PlayerQuitEvent) {
            val message = event.quitMessage() ?: return
            event.quitMessage(handle(message.esu, config.message.playerQuit)?.server)
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onQuit(event: PlayerAdvancementDoneEvent) {
            val message = event.message() ?: return
            event.message(handle(message.esu, config.message.doneAdvancement)?.server)
        }

        private fun handle(message: Component, modifier: ModuleConfig.Message.MessageModifier): Component? {
            val handler = { user: User ->
                user.message(
                    modifier.format,
                    component("message",
                        modifier.messageColor.applyTo(message) { message.color(it) }
                    )
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
        @io.github.rothes.esu.core.configuration.meta.Comment(
            """
            Customize the message behaviours.
            Set 'message-color' to '${OptionalSerializer.DISABLED}' or a color to override the default color of the vanilla message.
            Set 'format' to modify the original message, set it to empty to remove the messages.
            If 'show-in-console' is false, only online players can see the message.
            """, overrideOld = ["""
                Customize the message behaviours.
                Set 'color' to '${OptionalSerializer.DISABLED}' or a color to change the default color of the message.
                Set 'head' and 'foot' the prefix and suffix to be added to the original message.
                If 'show-in-console' is false, only the online players can see the message.
                """]
        )
        val message: Message = Message(),
    ): BaseModuleConfiguration() {

        data class Message(
            val death: MessageModifier = MessageModifier(TextColor.fromHexString("#dd9090")),
            val doneAdvancement: MessageModifier = MessageModifier(TextColor.fromHexString("#edde0e")),
            val playerJoin: MessageModifier = MessageModifier(NamedTextColor.GRAY, "<tc>[<pc>+<tc>] "),
            val playerQuit: MessageModifier = MessageModifier(NamedTextColor.GRAY, "<tc>[<pc>-<tc>] "),
        ): ConfigurationPart {

            data class MessageModifier(
                @RenamedFrom("color")
                val messageColor: Optional<TextColor> = Optional.empty(),
                val format: MessageData = "<message>".message,
                val showInConsole: Boolean = true,
            ): ConfigurationPart {
                @RemovedNode
                val color: Optional<TextColor>? = null
                @RemovedNode
                val head: String? = null
                @RemovedNode
                val foot: String? = null
                constructor(color: TextColor?, head: String = ""): this(Optional.ofNullable(color), "$head<message>".message)
            }

        }

    }

}