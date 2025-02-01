package io.github.rothes.esu.bukkit.module

import com.google.common.cache.CacheBuilder
import io.github.rothes.esu.bukkit.module.EsuChatModule.ModuleConfig.PrefixedMessageModifier
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MINECRAFT
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.data.SOUND
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.spongepowered.configurate.objectmapping.meta.Comment
import kotlin.collections.find
import kotlin.jvm.java
import kotlin.time.Duration
import kotlin.time.toJavaDuration

object EsuChatModule: BukkitModule<EsuChatModule.ModuleConfig, EsuChatModule.ModuleLocale>(
    ModuleConfig::class.java, ModuleLocale::class.java
) {

    const val WHISPER_COMMANDS = "msg|m|whisper|w|tell"
    const val REPLY_COMMANDS = "reply|r"
    const val EMOTE_COMMANDS = "emote|me"

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
        if (config.whisper.enabled)
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
                .build<User, LastTarget>()
            private val spying = hashSetOf<User>(ConsoleUser)

            @Command("$WHISPER_COMMANDS <receiver> <message>")
            fun whisper(sender: User, receiver: User, @Argument(parserName = "greedyString") message: String) {
                val msg = parseMessage(sender, message, config.whisper.prefixedMessageModifiers)
                sender.message(
                    locale, { whisper.formatOutgoing },
                    playerDisplay(sender, mapOf("sender" to sender, "receiver" to receiver)),
                    component("message", msg),
                    component("prefix", sender.buildMinimessage(locale, { whisper.prefix }))
                )
                receiver.message(
                    locale, { whisper.formatIncoming },
                    playerDisplay(receiver, mapOf("sender" to sender, "receiver" to receiver)),
                    component("message", msg),
                    component("prefix", receiver.buildMinimessage(locale, { whisper.prefix }))
                )
                val initiative = updateLast(sender, LastTarget(receiver, last.getIfPresent(receiver).let {
                    it == null || it.user != sender || !it.initiative
                })).initiative
                updateLast(receiver, LastTarget(sender, !initiative))
                for (user in spying) {
                    if (user.isOnline && user != sender && user != receiver)
                        user.message(
                            locale, { with(whisper.spy) { if (initiative) dmFormat else dmReplyFormat } },
                            playerDisplay(user, mapOf("sender" to sender, "receiver" to receiver)),
                            component("message", msg),
                            component("prefix", user.buildMinimessage(locale, { whisper.spy.prefix }))
                        )
                }
            }

            @Command("$REPLY_COMMANDS <message>")
            fun reply(sender: User, @Argument(parserName = "greedyString") message: String) {
                val last = last.getIfPresent(sender)?.user
                if (last == null) {
                    sender.message(
                        locale, { whisper.replyNoLastTarget },
                        component("prefix", sender.buildMinimessage(locale, { whisper.prefix }))
                    )
                    return
                }
                if (!last.isOnline) {
                    sender.message(
                        locale, { whisper.receiverOffline },
                        component("prefix", sender.buildMinimessage(locale, { whisper.prefix }))
                    )
                    return
                }
                whisper(sender, last, message)
            }

            @Command("spy")
            @Permission("esu.esuChat.spy")
            fun spyToggleShort(sender: User) {
                spyToggleShort(sender)
            }

            @Command("spy toggle")
            @Permission("esu.esuChat.spy")
            fun spyToggle(sender: User) {
                spyToggle(sender, sender)
            }

            @Command("spy enable")
            @Permission("esu.esuChat.spy")
            fun spyEnable(sender: User) {
                spyEnable(sender, sender)
            }

            @Command("spy disable")
            @Permission("esu.esuChat.spy")
            fun spyDisable(sender: User) {
                spyDisable(sender, sender)
            }

            @Command("spy toggle <user>")
            @Permission("esu.esuChat.spy")
            fun spyToggle(sender: User, target: User = sender) {

            }

            @Command("spy enable <user>")
            @Permission("esu.esuChat.spy")
            fun spyEnable(sender: User, target: User = sender) {

            }

            @Command("spy disable <user>")
            @Permission("esu.esuChat.spy")
            fun spyDisable(sender: User, target: User = sender) {

            }

            fun updateLast(user: User, lastTarget: LastTarget): LastTarget {
                val present = last.getIfPresent(user)
                val value = if (present?.user == lastTarget.user) present else lastTarget
                last.put(user, value)
                return value
            }

            data class LastTarget(
                val user: User,
                /**
                 * If the message is sent by sender firstly.
                 */
                val initiative: Boolean,
            )
        }

        object Emote {
            @Command("$EMOTE_COMMANDS <message>")
            fun emote(sender: User, @Argument(parserName = "greedyString") message: String) {
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

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun onChatCommand(event: PlayerCommandPreprocessEvent) {
            val message = event.message
            val split = message.split(' ', limit = 3)
            val command = split[0].substring(1).split(':').last().lowercase()
            if (EMOTE_COMMANDS.split('|').contains(command)) {
                if (config.emote.interceptNamespaces && split.size >= 2) {
                    ChatHandler.Emote.emote(event.player.user, split.drop(1).joinToString(separator = " "))
                    event.isCancelled = true
                }
            } else if (WHISPER_COMMANDS.split('|').contains(command)) {
                if (config.whisper.interceptNamespaces && split.size >= 3) {
                    ChatHandler.Whisper.whisper(event.player.user, Bukkit.getPlayer(split[1])?.user ?: return , split[2])
                    event.isCancelled = true
                }
            } else if (REPLY_COMMANDS.split('|').contains(command)) {
                if (config.whisper.interceptNamespaces && split.size >= 2) {
                    ChatHandler.Whisper.reply(event.player.user, split.drop(1).joinToString(separator = " "))
                    event.isCancelled = true
                }
            }
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
            component("message", Component.text(
                if (modifier != null && modifier.removePrefix) {
                    raw.drop(modifier.messagePrefix.length)
                } else {
                    raw
                }
            )),
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
        val whisper: Whisper = Whisper(),
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

        data class Whisper(
            @field:Comment("Enable esu whisper commands.")
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
        val whisper: Whisper = Whisper(),
        val emote: Emote = Emote(),
        val ignore: Ignore = Ignore(),
    ): ConfigurationPart {

        data class Chat(
            val format: MessageData = "\\<<player_display:sender>> <message>".message,
            @field:Comment("The format used for console logs.")
            val consoleFormat: MessageData = "<#48c0c0>\\<<player_display:sender>> <message>".message,
        ): ConfigurationPart

        data class Emote(
            val format: MessageData = "<pc>* <pdc><player_display:sender> <message>".message,
        ): ConfigurationPart

        data class Whisper(
            val prefix: String = "<sdc>📨 ",
            val formatIncoming: MessageData = "<prefix><pdc><player_display:sender><pc> <sc>➡ <tdc><message><$SOUND:$MINECRAFT:entity.silverfish.ambient:voice:0.2:2:-7007683334921848987>".message,
            val formatOutgoing: MessageData = "<prefix><sc>➡ <pc><pdc><player_display:receiver> <tc><message>".message,
            val replyNoLastTarget: MessageData = "<ec>There's no last direct message target.".message,
            val receiverOffline: MessageData = "<ec>The receiver is not online.".message,
            val spy: Spy = Spy(),
        ): ConfigurationPart {

            data class Spy(
                val prefix: String = "<sc>[<sdc>SPY<sc>] ",
                val dmFormat: MessageData = "<prefix><pc>[<pdc><player_display:sender> <sdc>➡ <tdc><player_display:receiver><pc>] <tc><message>".message,
                val dmReplyFormat: MessageData = "<prefix><pc>[<tdc><player_display:receiver> <sc>⬅ <pdc><player_display:sender><pc>] <tdc><message>".message,
            ): ConfigurationPart
        }

        data class Ignore(
            val prefix: String = "<sc>[<sdc>Ignore<sc>] ",
            val ignoringPlayer: MessageData = "<prefix><vnc>You are now ignoring <vndc><player><vnc>.".message,
            val receivingPlayer: MessageData = "<prefix><vpc>You are now receiving <vpdc><player><vpc>.".message,
        ): ConfigurationPart
    }

}