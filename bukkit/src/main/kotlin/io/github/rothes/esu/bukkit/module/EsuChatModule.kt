package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
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
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.spongepowered.configurate.objectmapping.meta.Comment

object EsuChatModule: BukkitModule<EsuChatModule.ModuleConfig, EsuChatModule.ModuleLocale>(
    ModuleConfig::class.java, ModuleLocale::class.java
) {

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
    }

    override fun disable() {
        super.disable()
        HandlerList.unregisterAll(Listeners)
    }

    object Listeners: Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun onPlayerChat(event: AsyncPlayerChatEvent) {
            val player = event.player
            val raw = event.message

            val modifier = config.chat.prefixedMessageModifiers.find {
                val perm = it.permission
                raw.length > it.messagePrefix.length // No blank message, thanks
                        && raw.startsWith(it.messagePrefix)
                        && (perm == null || perm.isEmpty() || player.hasPermission(perm))
            }

            val message = MiniMessage.miniMessage().deserialize("<head><message><foot>",
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

            for (user in Bukkit.getOnlinePlayers().map { it.user }.plus(ConsoleUser)) {
                user.message(locale, { chat.format }, playerDisplay(user, "sender", player), component("message", message))
            }

            event.isCancelled = true
        }

    }

    fun playerDisplay(viewer: User, key: String, player: Player): TagResolver {
        return TagResolver.resolver("player_display") { arg, context ->
            val pop = arg.pop()
            if (pop.value() == key)
                Tag.selfClosingInserting(
                    viewer.buildMinimessage(locale, { playerDisplay },
                        component("player_key", player.displayName()),
                        parsed("player_key_name", MiniMessage.miniMessage().escapeTags(player.name)))
                )
            else error("Unknown argument: $pop")
        }
    }

    data class ModuleConfig(
        val chat: Chat = Chat(),
        val emote: Emote = Emote(),
        val directMessage: DirectMessage = DirectMessage(),
    ): BaseModuleConfiguration() {

        data class Chat(
            val enableChatFormatting: Boolean = true,
            @field:Comment("""
If the message player sent starts with 'messagePrefix' and player has the permission,
the 'head' and 'foot' will be appended to the chat message.""")
            val prefixedMessageModifiers: List<PrefixedMessageModifier> = listOf(
                PrefixedMessageModifier(">", false, "", "<green>", "</green>"),
                PrefixedMessageModifier("*", true, "", "<gradient:#c8b3fd:#4bacc8>", "</gradient>"),
            )
        ): ConfigurationPart

        data class Emote(
            @field:Comment("Enable esu emote/me commands.")
            val enabled: Boolean = true,
            @field:Comment("Enabling this will redirect all emote commands to esu one, to avoid mixing usage.")
            val interceptNamespaces: Boolean = true,
        ): ConfigurationPart

        data class DirectMessage(
            @field:Comment("Enable esu direct message(a.k.a whisper) commands.")
            val enabled: Boolean = true,
            @field:Comment("Enabling this will redirect all whisper commands to esu one, to avoid mixing usage.")
            val interceptNamespaces: Boolean = true,
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
        val ignore: Ignore = Ignore(),
    ): ConfigurationPart {

        data class Chat(
            val format: String = "\\<<player_display:sender><reset>> <message>",
        ): ConfigurationPart

        data class Emote(
            val format: String = "<pc>* " +
                    "<player_display:sender>" +
                    "<reset> <message>",
        ): ConfigurationPart

        data class DirectMessage(
            val prefix: String = "<sc>[<sdc>DM<sc>] ",
            val formatIncoming: String = "<prefix><pc>[<pdc><player_display:receiver><pc>] <sc>-> <reset><message>",
            val formatOutgoing: String = "<prefix><sc>-> <pc>[<pdc><player_display:receiver><pc>] <reset><message>",
            val replyNoLastTarget: String = "<ec>There's no last direct message target."
        ): ConfigurationPart

        data class Ignore(
            val prefix: String = "<sc>[<sdc>Ignore<sc>] ",
            val ignoringPlayer: String = "<prefix><vnc>You are now ignoring <vndc><player><vnc>.",
            val receivingPlayer: String = "<prefix><vpc>You are now receiving <vpdc><player><vpc>.",
        ): ConfigurationPart
    }

}