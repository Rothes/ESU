package io.github.rothes.esu.bukkit.module

import com.google.common.cache.CacheBuilder
import io.github.rothes.esu.bukkit.event.*
import io.github.rothes.esu.bukkit.event.RawUserEmoteEvent.Companion.EMOTE_COMMANDS
import io.github.rothes.esu.bukkit.event.RawUserReplyEvent.Companion.REPLY_COMMANDS
import io.github.rothes.esu.bukkit.event.RawUserWhisperEvent.Companion.WHISPER_COMMANDS
import io.github.rothes.esu.bukkit.module.EsuChatModule.ModuleConfig.PrefixedMessageModifier
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.papi
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.user
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.displayName_
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MINECRAFT
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.data.SOUND
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.enabled
import io.github.rothes.esu.core.util.ComponentUtils.pLang
import io.github.rothes.esu.core.util.ComponentUtils.parsed
import io.github.rothes.esu.core.util.ComponentUtils.plainText
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.minimessage.MiniMessage
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.Tag
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.incendo.cloud.annotations.*
import java.util.concurrent.TimeUnit

object EsuChatModule: BukkitModule<EsuChatModule.ModuleConfig, EsuChatModule.ModuleLang>() {

    override fun onEnable() {
        Listeners.enable()
        if (config.whisper.enabled)
            registerCommands(ChatHandler.Whisper)
        if (config.emote.enabled)
            registerCommands(ChatHandler.Emote)

        for (player in Bukkit.getOnlinePlayers()) {
            ChatHandler.Whisper.checkSpyOnJoin(player.user)
        }
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.disable()
    }

    object ChatHandler {

        object Whisper {

            private val last = CacheBuilder.newBuilder()
                .expireAfterAccess(8, TimeUnit.HOURS)
                .build<User, LastTarget>()
            private val spying = hashSetOf<User>(ConsoleUser)

            @Command("$WHISPER_COMMANDS <receiver> <message>")
            fun whisper(sender: User, receiver: User, @Argument(parserName = "greedyString") message: String) {
                val parsed = parseMessage(sender, message, config.whisper.prefixedMessageModifiers)

                val papi = papi(sender)
                val msg = component("message", parsed)
                val pd = mapOf("sender" to sender, "receiver" to receiver)

                sender.message(config.whisper.formats.outgoing, msg,  papi,
                    playerDisplay(sender, pd),
                    pLang(sender, lang, { whisper.placeholders })
                )
                receiver.message(config.whisper.formats.incoming, msg, papi,
                    playerDisplay(receiver, pd),
                    pLang(receiver, lang, { whisper.placeholders })
                )
                val initiative = updateLast(sender, LastTarget(receiver, last.getIfPresent(receiver).let {
                    it == null || it.user != sender || !it.initiative
                })).initiative
                updateLast(receiver, LastTarget(sender, !initiative))
                for (user in spying) {
                    if (user.isOnline && user != sender && user != receiver)
                        user.message(
                            with(config.whisper.formats.spy) { if (initiative) send else reply },
                            msg, papi,
                            playerDisplay(user, pd),
                            pLang(user, lang, { whisper.spy.placeholders })
                        )
                }
            }

            @Command("$REPLY_COMMANDS <message>")
            fun reply(sender: User, @Argument(parserName = "greedyString") message: String) {
                val last = last.getIfPresent(sender)?.user
                if (last == null) {
                    sender.message(
                        lang, { whisper.replyNoLastTarget },
                        pLang(sender, lang, { whisper.placeholders }),
                    )
                    return
                }
                if (!last.isOnline) {
                    sender.message(
                        lang, { whisper.receiverOffline },
                        pLang(sender, lang, { whisper.placeholders }),
                    )
                    return
                }
                whisper(sender, last, message)
            }

            @Commands(Command("spy"), Command("spy toggle"))
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
            @Permission("esu.esuChat.spy.other")
            fun spyToggle(sender: User, user: User = sender, @Flag("silent") silent: Boolean = false) {
                if (!spying.contains(user)) {
                    spyEnable(sender, user, silent)
                } else {
                    spyDisable(sender, user, silent)
                }
            }

            @Command("spy enable <user>")
            @Permission("esu.esuChat.spy.other")
            fun spyEnable(sender: User, user: User = sender, @Flag("silent") silent: Boolean = false) {
                val added = spying.add(user)
                if (added) {
                    sender.message(lang, { whisper.spy.enabled },
                        pLang(sender, lang, { whisper.spy.placeholders }),
                        user(user, "user"), component("enable-state", true.enabled(sender)) )
                    if (!silent && sender != user) {
                        user.message(lang, { whisper.spy.enabled },
                            pLang(sender, lang, { whisper.spy.placeholders }),
                            user(user, "user"), component("enable-state", true.enabled(sender)) )
                    }
                } else {
                    sender.message(lang, { whisper.spy.alreadyEnabled },
                        pLang(sender, lang, { whisper.spy.placeholders }),
                        user(user, "user"))
                }
            }

            @Command("spy disable <user>")
            @Permission("esu.esuChat.spy.other")
            fun spyDisable(sender: User, user: User = sender, @Flag("silent") silent: Boolean = false) {
                val removed = spying.remove(user)
                if (removed) {
                    sender.message(lang, { whisper.spy.disabled },
                        pLang(sender, lang, { whisper.spy.placeholders }),
                        user(user, "user"), component("enable-state", false.enabled(sender)) )
                    if (!silent && sender != user)
                        user.message(lang, { whisper.spy.disabled },
                            pLang(sender, lang, { whisper.spy.placeholders }),
                            user(user, "user"), component("enable-state", false.enabled(sender)) )
                } else {
                    sender.message(lang, { whisper.spy.alreadyDisabled },
                        pLang(sender, lang, { whisper.spy.placeholders }),
                        user(user, "user"))
                }
            }

            fun checkSpyOnJoin(user: User) {
                if (user.hasPerm("spy.enableOnJoin")) {
                    spyEnable(user, user, true)
                }
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
                val papi = papi(sender)

                for (user in Bukkit.getOnlinePlayers().map { it.user }.plus(ConsoleUser)) {
                    val tags = arrayOf(
                        playerDisplay(user, "sender", sender), component("message", msg), papi
                    )
                    user.message(config.emote.format, pLang(user, lang, { emote.placeholders }), *tags)
                }
            }
        }

        object Chat {

            fun chat(sender: User, message: String) {
                chat(sender, Component.text(message))
            }

            fun chat(sender: User, message: Component) {
                var isShout = false
                val rangedChat = config.chat.rangedChat
                val shoutHandled = if (rangedChat.enabled) {
                    val plainText = message.plainText
                    if (plainText != rangedChat.shoutPrefix && plainText.startsWith(rangedChat.shoutPrefix) && sender.hasPerm("chat.shout")) {
                        isShout = true
                        message.drop(rangedChat.shoutPrefix.length)
                    } else {
                        message
                    }
                } else {
                    message
                }

                val msg = parseMessage(sender, shoutHandled, config.chat.prefixedMessageModifiers)

                broadcastChat(sender, msg, isShout)
            }

            private fun broadcastChat(sender: User, msg: Component, shout: Boolean) {
                val config = config.chat
                val format = if (config.rangedChat.enabled && shout) config.rangedChat.shoutFormat else config.format
                val users = Bukkit.getOnlinePlayers()
                    .filter { player ->
                        if (!config.rangedChat.enabled || shout || sender !is PlayerUser) {
                            true
                        } else {
                            val sl = sender.player.location
                            val pl = player.location
                            sl.world == pl.world && sl.distanceSquared(pl) <= config.rangedChat.radius * config.rangedChat.radius
                        }
                    }
                    .map { it.user }

                val message = component("message", msg)
                val papi = papi(sender)
                for (user in users) {
                    user.message(
                        format.player, message, papi,
                        pLang(user, lang, { chat.placeholders }),
                        playerDisplay(user, "sender", sender)
                    )
                }

                ConsoleUser.message(
                    format.console, message, papi,
                    pLang(ConsoleUser, lang, { chat.placeholders }),
                    playerDisplay(ConsoleUser, "sender", sender)
                )
            }
        }
    }

    object Listeners: Listener {

        fun enable() {
            Listeners.register()
        }

        fun disable() {
            Listeners.unregister()
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun onChat(e: RawUserChatEvent) {
            if (!config.chat.enabled)
                return

            ChatHandler.Chat.chat(e.user, e.message)

            e.isCancelled = true
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun onEmote(e: RawUserEmoteEvent) {
            if (config.emote.interceptNamespaces) {
                ChatHandler.Emote.emote(e.user, e.message)
                e.isCancelled = true
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun onWhisper(e: RawUserWhisperEvent) {
            if (config.whisper.interceptNamespaces) {
                val target = e.targetPlayer?.user ?: return
                ChatHandler.Whisper.whisper(e.user, target, e.message)
                e.isCancelled = true
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun onReply(e: RawUserReplyEvent) {
            if (config.whisper.interceptNamespaces) {
                ChatHandler.Whisper.reply(e.user, e.message)
                e.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerJoin(e: UserLoginEvent) {
            ChatHandler.Whisper.checkSpyOnJoin(e.user)
        }

        @EventHandler
        fun onPlayerLeave(e: PlayerQuitEvent) {
            val user = e.player.user
            ChatHandler.Whisper.spyDisable(user, user, true)
        }

    }

    fun parseMessage(sender: User, raw: String, modifiers: List<PrefixedMessageModifier>): Component {
        val modifier = matchModifier(sender, raw, modifiers) ?: return Component.text(raw)

        return MiniMessage.miniMessage().deserialize(modifier.format,
            component("message", Component.text(
                if (modifier.removePrefix) {
                    raw.drop(modifier.messagePrefix.length)
                } else {
                    raw
                }
            )),
        )
    }

    fun parseMessage(sender: User, raw: Component, modifiers: List<PrefixedMessageModifier>): Component {
        val modifier = matchModifier(sender, raw.plainText, modifiers) ?: return raw

        return MiniMessage.miniMessage().deserialize(modifier.format,
            component("message",
                if (modifier.removePrefix) {
                    val times = modifier.messagePrefix.length
                    raw.drop(times)
                } else {
                    raw
                }
            ),
        )
    }

    fun Component.drop(n: Int): Component {
        return replaceText {
            it.match(".".toPattern()).replacement("").times(n)
        }
    }

    fun Component.setItem(sender: User): Component {
        return if (sender is PlayerUser)
            replaceText {
                it.matchLiteral("[i]")
                    .replacement { _ -> sender.player.inventory.itemInMainHand.displayName().esu }
            }
        else this
    }

    private fun matchModifier(sender: User, text: String, modifiers: List<PrefixedMessageModifier>): PrefixedMessageModifier? {
        return modifiers.find {
            val perm = it.permission
            (!it.removePrefix || text.length > it.messagePrefix.length) // No blank message, thanks
                    && text.startsWith(it.messagePrefix)
                    && (perm.isNullOrEmpty() || sender.hasPermission(perm))
        }
    }

    fun playerDisplay(viewer: User, key: String, user: User): TagResolver {
        return playerDisplay(viewer, mapOf(key to user))
    }

    fun playerDisplay(viewer: User, map: Map<String, User>): TagResolver {
        return TagResolver.resolver(setOf("pd", "player_display")) { arg, context ->
            val pop = arg.popOr("One argument required for player_display")
            val id = pop.value()
            val user = map[id]
            if (user != null)
                Tag.selfClosingInserting(
                    viewer.buildMiniMessage(lang, { playerDisplay },
                        papi(user),
                        if (user is PlayerUser)
                            component("player_key", user.player.displayName_)
                        else
                            parsed("player_key", MiniMessage.miniMessage().escapeTags(user.name)),
                        parsed("player_key_name", MiniMessage.miniMessage().escapeTags(user.name)))
                )
            else {
                throw context.newException("Unknown player_display argument: $id")
            }
        }
    }

    data class ModuleConfig(
        val chat: Chat = Chat(),
        val emote: Emote = Emote(),
        val whisper: Whisper = Whisper(),
    ): BaseModuleConfiguration() {

        data class Chat(
            val enabled: Boolean = true,
            @Comment("'player' is what players see, and 'console' is what printed to console.")
            val format: ChatFormat = ChatFormat(
                "\\<<pd:sender>> <message>".message,
                "<#48c0c0>\\<<pd:sender>> <message>".message,
            ),
            @Comment("""
                By enabling this, players only receive chat messages from nearby players, of the same world.
            """)
            val rangedChat: RangedChat = RangedChat(),
            @Comment("""
                If the message player sent starts with 'messagePrefix' and player has the permission,
                the chat message will become 'format' .
            """, overrideOld = ["""
                If the message player sent starts with 'messagePrefix' and player has the permission,
                the 'head' and 'foot' will be appended to the chat message.
            """])
            val prefixedMessageModifiers: List<PrefixedMessageModifier> = listOf(
                PrefixedMessageModifier(">", false, "", "<green>", "</green>"),
                PrefixedMessageModifier("*", true, "", "<gradient:#c8b3fd:#4bacc8>", "</gradient>"),
            ),
        ) {
            data class RangedChat(
                val enabled: Boolean = false,
                val radius: Int = 5000,
                @Comment("""
                    Player with `esu.esuchat.chat.shout` permission can use this perfix to bypass ranged chat.
                    By default, players own this permission.
                    Use this prefix before prefixedMessageModifiers.
                """)
                val shoutPrefix: String = "!",
                val shoutFormat: ChatFormat = ChatFormat(
                    "\\<<pd:sender>> <pl:shout><message>".message,
                    "<#48c0c0>\\<<pd:sender>> <pl:shout><message>".message,
                ),
            )

            data class ChatFormat(
                val player: MessageData = MessageData(),
                val console: MessageData = MessageData(),
            )
        }

        data class Emote(
            @Comment("Enable esu emote/me commands.")
            val enabled: Boolean = true,
            val format: MessageData = "<pl:prefix><pdc><pd:sender></pdc> <message>".message,
            @Comment("Enabling this will redirect all emote commands to the esu one, to avoid mixing usage.")
            val interceptNamespaces: Boolean = true,
            val prefixedMessageModifiers: List<PrefixedMessageModifier> = listOf(
                PrefixedMessageModifier("", true, "", "<gray>", "</gray>"),
            ),
        )

        data class Whisper(
            @Comment("Enable esu whisper commands.")
            val enabled: Boolean = true,
            val formats: Formats = Formats(),

            val interceptNamespaces: Boolean = true,
            val prefixedMessageModifiers: List<PrefixedMessageModifier> = listOf(
                PrefixedMessageModifier(">", false, "", "<green>", "</green>"),
                PrefixedMessageModifier("*", true, "", "<gradient:#c8b3fd:#4bacc8>", "</gradient>"),
            ),
        ) {
            data class Formats(
                val incoming: MessageData = "<pl:prefix><pdc><pd:sender> <sc>➡ <tdc><message><$SOUND:$MINECRAFT:entity.silverfish.ambient:voice:0.2:2:-7007683334921848987>".message,
                val outgoing: MessageData = "<pl:prefix><sc>➡ <pdc><pd:receiver> <tc><message>".message,
                val spy: Spy = Spy()
            ) {
                data class Spy(
                    val send: MessageData = "<pl:prefix><pc>[<pdc><pd:sender> <sdc>➡ <tdc><pd:receiver><pc>] <tc><message>".message,
                    val reply: MessageData = "<pl:prefix><pc>[<tdc><pd:receiver> <sc>⬅ <pdc><pd:sender><pc>] <tdc><message>".message,
                )
            }
        }

        data class PrefixedMessageModifier(
            val messagePrefix: String = "",
            val removePrefix: Boolean = false,
            val permission: String? = "",
            val format: String = "<message>"
        ) {
            constructor(
                messagePrefix: String = "",
                removePrefix: Boolean = false,
                permission: String? = "",
                head: String = "",
                foot: String = "",
            ) : this(messagePrefix, removePrefix, permission, "$head<message>$foot")
        }
    }

    data class ModuleLang(
        @Comment("This is being used with <pd:player_key>.")
        val playerDisplay: String = "<hover:show_text:'<pc>Click to whisper <pdc><player_key>'>" +
                "<click:suggest_command:/m <player_key_name> ><player_key></hover>",
        val chat: Chat = Chat(),
        val emote: Emote = Emote(),
        val whisper: Whisper = Whisper(),
        val ignore: Ignore = Ignore(),
    ): ConfigurationPart {

        data class Chat(
            val placeholders: Map<String, String> = mapOf(
                "shout" to "<pc><hover:show_text:'<pc>Shout chat message'>📣 </hover>",
            ),
        )

        data class Emote(
            val placeholders: Map<String, String> = mapOf(
                "prefix" to "<pc><hover:show_text:'<pc>Emote message'>* </hover>",
            ),
        )

        data class Whisper(
            val placeholders: Map<String, String> = mapOf(
                "prefix" to "<sdc><hover:show_text:'<pc>Whisper channel'>📨 </hover>",
            ),
            val replyNoLastTarget: MessageData = "<ec>There's no last direct message target.".message,
            val receiverOffline: MessageData = "<ec>The receiver is not online.".message,
            val spy: Spy = Spy(),
        ) {

            data class Spy(
                val placeholders: Map<String, String> = mapOf(
                    "prefix" to "<sc>[<sdc>SPY<sc>] ",
                ),
                val enabled: MessageData = "<pl:prefix><pdc>Enabled <pc>spy for <pdc><user></pdc>.".message,
                val disabled: MessageData = "<pl:prefix><pdc>Disabled <pc>spy for <pdc><user></pdc>.".message,
                val alreadyEnabled: MessageData = "<pl:prefix><edc><user> <ec>has already enabled spy.".message,
                val alreadyDisabled: MessageData = "<pl:prefix><edc><user> <ec>has already disabled spy.".message,
            )
        }

        data class Ignore(
            val placeholders: Map<String, String> = mapOf(
                "prefix" to "<sc>[<sdc>Ignore<sc>] ",
            ),
            val ignoringPlayer: MessageData = "<pl:prefix><nc>You are now <vnc>ignoring</vnc> <pdc><player></pdc>.".message,
            val receivingPlayer: MessageData = "<pl:prefix><pc>You are now <vpc>receiving</vpc> <pdc><player></pdc>.".message,
        )
    }

}