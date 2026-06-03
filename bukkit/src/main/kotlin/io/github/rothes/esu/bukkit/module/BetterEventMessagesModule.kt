/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.event.RichPlayerDeathEvent
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.ServerInfo
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
import io.github.rothes.esu.core.util.extension.charSize
import io.github.rothes.esu.core.util.extension.substringCharSize
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.TextComponent
import io.github.rothes.esu.lib.adventure.text.TranslatableComponent
import io.github.rothes.esu.lib.adventure.text.TranslationArgument
import io.github.rothes.esu.lib.adventure.text.event.HoverEvent
import io.github.rothes.esu.lib.adventure.text.format.NamedTextColor
import io.github.rothes.esu.lib.adventure.text.format.TextColor
import io.github.rothes.esu.lib.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrElse

object BetterEventMessagesModule: BukkitModule<BetterEventMessagesModule.ModuleConfig, EmptyConfiguration>() {

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (!ServerInfo.isPaper) return errFail { "Requires Paper".message }
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
        fun onDeath(e: PlayerDeathEvent) {
            val server = e.deathMessage() ?: return
            if (server !is net.kyori.adventure.text.TranslatableComponent) return // Only support vanilla messages

            val config = config
            val maxItemName = config.maxDeathItemNameSize.getOrDefault(-1)
            val maxEntityName = config.maxDeathEntityNameSize.getOrDefault(-1)
            if (maxItemName < 0 && maxEntityName < 0) return

            val msg = server.esu as TranslatableComponent
            var modified = false
            val arguments = msg.arguments().toMutableList()
            for ((i, arg) in arguments.map { it.value() }.withIndex()) {
                fun Component.limitCharSize(size: Int): Component {
                    return replaceText {
                        var remain = size
                        it.match(".+").replacement { c ->
                            val sz = c.content().charSize()
                            if (sz <= remain) // Accepts all chars
                                c.also { remain -= sz }
                            else if (remain >= 0) // Print ".." in this scope
                                c.content(c.content().substringCharSize(0, remain))
                                    .append(Component.text("..", NamedTextColor.GRAY))
                                    .also { remain = -1 } // No longer accept chars anymore
                            else // Skip this component
                                null
                        }
                    }
                }
                fun setArg(i: Int, component: Component) {
                    arguments[i] = TranslationArgument.component(component)
                    modified = true
                }

                if (maxItemName >= 0 && arg is TranslatableComponent && arg.key() == "chat.square_brackets") {
                    val argument = arg.arguments()[0]
                    val item = argument.value() as? TextComponent ?: continue
                    if (arg.hoverEvent()?.value() !is HoverEvent.ShowItem) continue // Ensure it's item
                    if (!item.hasDecoration(TextDecoration.ITALIC)) continue // Not a custom name

                    val component = item.children().firstOrNull() as? TextComponent ?: continue
                    val content = component.content()
                    if (content.charSize() <= maxItemName) continue
                    setArg(i, arg.arguments(item.limitCharSize(maxItemName)))
                } else if (maxEntityName >= 0 && arg is TextComponent) {
                    val showEntity = arg.hoverEvent()?.value() as? HoverEvent.ShowEntity ?: continue // Ensure it's entity
                    if (showEntity.type().value() == "player") continue // Skip for player name

                    setArg(i, arg.limitCharSize(maxEntityName))
                }
            }

            if (modified) e.deathMessage(msg.arguments(arguments).server)
        }

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
        @Comment("""
            For death message, limit the displayed name length of the item which caused the kill.
        """)
        val maxDeathItemNameSize: Optional<Int> = Optional.empty(),
        val maxDeathEntityNameSize: Optional<Int> = Optional.empty(),
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