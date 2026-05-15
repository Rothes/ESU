package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.event.RichPlayerDeathEvent
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.meta.RemovedNode
import io.github.rothes.esu.core.configuration.meta.RenamedFrom
import io.github.rothes.esu.core.configuration.serializer.OptionalSerializer
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.OptionalUtils.applyTo
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.TranslatableComponent
import io.github.rothes.esu.lib.adventure.text.format.NamedTextColor
import io.github.rothes.esu.lib.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import kotlin.jvm.optionals.getOrElse

object BetterEventMessagesModule: BukkitModule<BetterEventMessagesModule.ModuleConfig, EmptyConfiguration>() {

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (!ServerCompatibility.isPaper) return errFail { "Requires Paper".message }
            null
        }
    }

    override fun onEnable() {
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
    }

    object Listeners: Listener {

        @EventHandler(priority = EventPriority.NORMAL)
        fun onDeath(e: RichPlayerDeathEvent) {
            e.setChatMessage { user, old ->
                old?.let { msg ->
                    processComponent(user, msg, config.message.death)
                }
            }
        }

        @EventHandler(priority = EventPriority.NORMAL)
        fun onJoin(event: PlayerJoinEvent) {
            val message = event.joinMessage() ?: return
            val esu = message.esu

            val modifier =
                if (esu is TranslatableComponent && esu.key() == "multiplayer.player.joined.renamed")
                    config.message.playerJoinRenamed.getOrElse { config.message.playerJoin }
                else
                    config.message.playerJoin

            event.joinMessage(handle(esu, modifier)?.server)
        }

        @EventHandler(priority = EventPriority.NORMAL)
        fun onQuit(event: PlayerQuitEvent) {
            val message = event.quitMessage() ?: return
            event.quitMessage(handle(message.esu, config.message.playerQuit)?.server)
        }

        @EventHandler(priority = EventPriority.NORMAL)
        fun onAdvancementDone(event: PlayerAdvancementDoneEvent) {
            val message = event.message() ?: return
            event.message(handle(message.esu, config.message.doneAdvancement)?.server)
        }

        private fun processComponent(user: User, message: Component, modifier: ModuleConfig.Message.MessageModifier): Component? {
            if (modifier.format.isEmpty()) return null
            return user.buildMiniMessage(
                modifier.format,
                component("message",
                    modifier.messageColor.applyTo(message) { message.color(it) }
                )
            )
        }

        private fun handle(message: Component, modifier: ModuleConfig.Message.MessageModifier): Component? {
            val handler = { user: User ->
                processComponent(user, message, modifier)?.let { user.message(it) }
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
        @Comment(
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
            @Comment("Modifier for `multiplayer.player.joined.renamed` specifically")
            val playerJoinRenamed: Optional<MessageModifier> = Optional.empty(),
            val playerQuit: MessageModifier = MessageModifier(NamedTextColor.GRAY, "<tc>[<pc>-<tc>] "),
        ): ConfigurationPart {

            data class MessageModifier(
                @RenamedFrom("color")
                val messageColor: Optional<TextColor> = Optional.empty(),
                val format: String = "<message>",
                val showInConsole: Boolean = true,
            ): ConfigurationPart {
                @RemovedNode
                val color: Optional<TextColor>? = null
                @RemovedNode
                val head: String? = null
                @RemovedNode
                val foot: String? = null
                constructor(color: TextColor?, head: String = ""): this(Optional.ofNullable(color), "$head<message>")
            }

        }

    }

}