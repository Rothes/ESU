package io.github.rothes.esu.bukkit.module

import com.google.common.cache.CacheBuilder
import io.github.rothes.esu.bukkit.module.EsuChatModule.ModuleConfig.PrefixedMessageModifier
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.parsed
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.incendo.cloud.annotations.Command
import org.spongepowered.configurate.objectmapping.meta.Comment
import kotlin.time.Duration
import kotlin.time.toJavaDuration

object EsuChatModule: BukkitModule<EsuChatModule.ModuleConfig, EsuChatModule.ModuleLocale>(
    ModuleConfig::class.java, ModuleLocale::class.java
) {

    const val WHISPER_COMMANDS = "message|msg|m|whisper|w|tell|dm|pm"
    const val REPLY_COMMANDS = "reply|r|last|l"
    const val EMOTE_COMMANDS = "emote|me"

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
        if (config.directMessage.enabled)
            registerCommands(ChatHandler.Whisper)
        if (config.emote.enabled)
            registerCommands(ChatHandler.Emote)
    }

    override fun disable() {
        super.disable()
        HandlerList.unregisterAll(Listeners)
    }

    object ChatHandler {

        object Whisper {

            private val last = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.parse("8h").toJavaDuration())
                .build<User, User>()

            @Command("$WHISPER_COMMANDS <receiver> <message>")
            fun whisper(sender: User, receiver: User, message: String) {
                val msg = parseMessage(sender, message, config.directMessage.prefixedMessageModifiers)
                sender.message(
                    locale, { directMessage.formatOutgoing },
                    playerDisplay(sender, mapOf("sender" to sender, "receiver" to receiver)),
                    component("message", msg),
                    component("prefix", sender.buildMinimessage(locale, { directMessage.prefix }))
                )
                receiver.message(
                    locale, { directMessage.formatIncoming },
                    playerDisplay(receiver, mapOf("sender" to sender, "receiver" to receiver)),
                    component("message", msg),
                    component("prefix", receiver.buildMinimessage(locale, { directMessage.prefix }))
                )
                last.put(sender, receiver)
                last.put(receiver, sender)
            }

            @Command("$REPLY_COMMANDS <message>")
            fun reply(sender: User, message: String) {
                val last = last.getIfPresent(sender)
                if (last == null) {
                    sender.message(
                        locale, { directMessage.replyNoLastTarget },
                        component("prefix", sender.buildMinimessage(locale, { directMessage.prefix }))
                    )
                    return
                }
                if (!last.isOnline) {
                    sender.message(
                        locale, { directMessage.receiverOffline },
                        component("prefix", sender.buildMinimessage(locale, { directMessage.prefix }))
                    )
                    return
                }
                whisper(sender, last, message)
            }
        }

        object Emote {
            @Command("$EMOTE_COMMANDS <message>")
            fun emote(sender: User, message: String) {
                val msg = parseMessage(sender, message, config.emote.prefixedMessageModifiers)

                for (user in Bukkit.getOnlinePlayers().map { it.user }.plus(ConsoleUser)) {
                    user.message(locale, { emote.format }, playerDisplay(user, "sender", sender), component("message", msg))
                }
            }
        }

        object Chat {

            fun chat(sender: User, message: String) {
                val msg = parseMessage(sender, message, config.chat.prefixedMessageModifiers)

                for (user in Bukkit.getOnlinePlayers().map { it.user }) {
                    user.message(
                        locale, { chat.format },
                        playerDisplay(user, "sender", sender),
                        component("message", msg)
                    )
                }
                ConsoleUser.message(
                    locale, { chat.consoleFormat },
                    playerDisplay(ConsoleUser, "sender", sender),
                    component("message", msg)
                )
            }
        }
    }

    object Listeners: Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun onPlayerChat(event: AsyncPlayerChatEvent) {
            if (!config.chat.enableChatFormatting)
                return

            ChatHandler.Chat.chat(event.player.user, event.message)

            event.isCancelled = true
        }

    }

    fun parseMessage(sender: User, raw: String, modifiers: List<PrefixedMessageModifier>): Component {
        val modifier = modifiers.find {
            val perm = it.permission
            (!it.removePrefix || raw.length > it.messagePrefix.length) // No blank message, thanks
                    && raw.startsWith(it.messagePrefix)
                    && (perm == null || perm.isEmpty() || sender.hasPermission(perm))
        }

        return MiniMessage.miniMessage().deserialize("<head><message><foot>",
            TagResolver.resolver("message", Tag.inserting(Component.text(
                if (modifier != null && modifier.removePrefix) {
                    raw.drop(modifier.messagePrefix.length)
                } else {
                    raw
                }
            ))),
            parsed("head", modifier?.head ?: ""),
            parsed("foot", modifier?.foot ?: ""),
        )
    }

    fun playerDisplay(viewer: User, key: String, user: User): TagResolver {
        return playerDisplay(viewer, mapOf(key to user))
    }

    fun playerDisplay(viewer: User, map: Map<String, User>): TagResolver {
        return TagResolver.resolver("player_display") { arg, context ->
            val pop = arg.pop()
            val user = map[pop.value()]
            if (user != null)
                Tag.selfClosingInserting(
                    viewer.buildMinimessage(locale, { playerDisplay },
                        if (user is PlayerUser)
                            component("player_key", user.player.displayName())
                        else
                            parsed("player_key", MiniMessage.miniMessage().escapeTags(user.name)),
                        parsed("player_key_name", MiniMessage.miniMessage().escapeTags(user.name)))
                )
            else
                error("Unknown argument: $pop")
        }
    }

    data class ModuleConfig(
        val chat: Chat = Chat(),
        val directMessage: DirectMessage = DirectMessage(),
        val emote: Emote = Emote(),
    ): BaseModuleConfiguration() {

        data class Chat(
            val enableChatFormatting: Boolean = true,
            @field:Comment("""
If the message player sent starts with 'messagePrefix' and player has the permission,
the 'head' and 'foot' will be appended to the chat message.""")
            val prefixedMessageModifiers: List<PrefixedMessageModifier> = listOf(
                PrefixedMessageModifier(">", false, "", "<green>", "</green>"),
                PrefixedMessageModifier("*", true, "", "<gradient:#c8b3fd:#4bacc8>", "</gradient>"),
            ),
        ): ConfigurationPart

        data class DirectMessage(
            @field:Comment("Enable esu direct message(a.k.a whisper) commands.")
            val enabled: Boolean = true,
            val interceptNamespaces: Boolean = true,
            val prefixedMessageModifiers: List<PrefixedMessageModifier> = listOf(
                PrefixedMessageModifier(">", false, "", "<green>", "</green>"),
                PrefixedMessageModifier("*", true, "", "<gradient:#c8b3fd:#4bacc8>", "</gradient>"),
            ),
        ): ConfigurationPart

        data class Emote(
            @field:Comment("Enable esu emote/me commands.")
            val enabled: Boolean = true,
            @field:Comment("Enabling this will redirect all emote commands to esu one, to avoid mixing usage.")
            val interceptNamespaces: Boolean = true,
            val prefixedMessageModifiers: List<PrefixedMessageModifier> = listOf(
                PrefixedMessageModifier(">", false, "", "<green>", "</green>"),
                PrefixedMessageModifier("*", true, "", "<gradient:#c8b3fd:#4bacc8>", "</gradient>"),
                PrefixedMessageModifier("", true, "", "<gray>", "</gray>"),
            ),
        ): ConfigurationPart

        data class PrefixedMessageModifier(
            val messagePrefix: String = "",
            val removePrefix: Boolean = false,
            val permission: String? = "",
            val head: String = "",
            val foot: String = "",
        ): ConfigurationPart
    }

    data class ModuleLocale(
        @field:Comment("This is being used with <player_display:player_key> below.")
        val playerDisplay: String = "<hover:show_text:'<pc>Click to whisper <pdc><player_key>'>" +
                "<click:suggest_command:/m <player_key_name> ><player_key></hover>",
        val chat: Chat = Chat(),
        val directMessage: DirectMessage = DirectMessage(),
        val emote: Emote = Emote(),
        val ignore: Ignore = Ignore(),
    ): ConfigurationPart {

        data class Chat(
            val format: String = "\\<<player_display:sender>> <message>",
            @field:Comment("The format used for console logs.")
            val consoleFormat: String = "<#48c0c0>\\<<player_display:sender>> <message>"
        ): ConfigurationPart

        data class Emote(
            val format: String = "<pc>* " +
                    "<player_display:sender>" +
                    "<reset> <message>",
        ): ConfigurationPart

        data class DirectMessage(
            val prefix: String = "<sc>[<sdc>DM<sc>] ",
            val formatIncoming: String = "<prefix><pc>[<pdc><player_display:sender><pc>] <sc>-> <reset><message>",
            val formatOutgoing: String = "<prefix><sc>-> <pc>[<pdc><player_display:receiver><pc>] <reset><message>",
            val replyNoLastTarget: String = "<ec>There's no last direct message target.",
            val receiverOffline: String = "<ec>The receiver is not online.",
        ): ConfigurationPart

        data class Ignore(
            val prefix: String = "<sc>[<sdc>Ignore<sc>] ",
            val ignoringPlayer: String = "<prefix><vnc>You are now ignoring <vndc><player><vnc>.",
            val receivingPlayer: String = "<prefix><vpc>You are now receiving <vpdc><player><vpc>.",
        ): ConfigurationPart
    }

}