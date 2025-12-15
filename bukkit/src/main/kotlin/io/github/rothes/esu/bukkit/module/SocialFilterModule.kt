package io.github.rothes.esu.bukkit.module

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie
import io.github.rothes.esu.bukkit.event.RawUserChatEvent
import io.github.rothes.esu.bukkit.event.RawUserEmoteEvent
import io.github.rothes.esu.bukkit.event.RawUserWhisperEvent
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.displayName_
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.meta
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.plainText
import io.github.rothes.esu.core.util.extension.ifLet
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.AnvilInventory

object SocialFilterModule: BukkitModule<BaseModuleConfiguration, SocialFilterModule.ModuleLang>() {

    lateinit var filters: MultiConfiguration<Filter>
        private set

    override fun onEnable() {
        Listeners.register()
    }

    override fun onDisable() {
        Listeners.unregister()
    }

    override fun onReload() {
        super.onReload()
        filters = ConfigLoader.loadMulti(
            moduleFolder.resolve("filters"),
            ConfigLoader.LoaderSettingsMulti(
                initializeConfigs = listOf("example")
            )
        )
    }

    private object Listeners : Listener {

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onAnvilRename(e: InventoryClickEvent) {
            val inventory = e.inventory
            if (inventory !is AnvilInventory) return
            val item = inventory.result ?: return

            item.meta { meta ->
                val name = meta.displayName_?.plainText ?: return
                val find = filters.configs.values.find {
                    it.enabled && it.blockAnvilRename && it.contains(name)
                } ?: return

                e.isCancelled = true
                find.messageBlocked((e.whoClicked as Player).user)
            }
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onChat(e: RawUserChatEvent) {
            val message = e.message.plainText
            val find = filters.configs.values.find {
                it.enabled && it.blockChat && it.contains(message)
            } ?: return

            e.isCancelled = true
            find.messageBlocked(e.user)
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onEmote(e: RawUserEmoteEvent) {
            val message = e.message
            val find = filters.configs.values.find {
                it.enabled && it.blockChat && it.contains(message)
            } ?: return

            e.isCancelled = true
            find.messageBlocked(e.user)
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onWhisper(e: RawUserWhisperEvent) {
            val message = e.message
            val find = filters.configs.values.find {
                it.enabled && it.blockChat && it.contains(message)
            } ?: return

            e.isCancelled = true
            find.messageBlocked(e.user)
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onSign(e: SignChangeEvent) {
            @Suppress("DEPRECATION")
            val message = e.lines.joinToString("")
            val find = filters.configs.values.find {
                it.enabled && it.blockSign && it.contains(message)
            } ?: return

            e.isCancelled = true
            find.messageBlocked(e.player.user)
        }

    }

    data class Filter(
        @Comment("Enable this filter file.")
        val enabled: Boolean = false,
        @Comment("Block renaming items in anvils.")
        val blockAnvilRename: Boolean = true,
        @Comment("Block chat texts, including chat/emote/whisper .")
        val blockChat: Boolean = true,
        @Comment("Block writing texts on sign blocks.")
        val blockSign: Boolean = true,
        @Comment("Ignore case on matching texts.")
        val ignoreCase: Boolean = true,
        @Comment("Normalize text to fix bypassing by characters like blank.")
        val normalizeText: Boolean = true,
        @Comment("""
            The message to send when blocked by this file.
            This is the key in the 'blocked-message' map in lang files.
        """)
        val blockedMessageKey: String = "bad-keywords",
        val keywords: List<String> = listOf("A keyword to block", "Another keyword to block"),
    ) : ConfigurationPart {

        val searcher by lazy {
            AhoCorasickDoubleArrayTrie<Filter>().also { trie ->
                trie.build(keywords.map { preprocessText(it) }.associateWith { this })
            }
        }

        fun contains(text: String): Boolean {
            return searcher.findFirst(preprocessText(text)) != null
        }

        fun messageBlocked(user: User) {
            if (blockedMessageKey.isEmpty()) return
            val message = user.localedOrNull(lang) { blockedMessage[blockedMessageKey] } ?: blockedMessageKey.message
            user.message(message)
        }

        fun preprocessText(text: String): String {
            return text
                .ifLet(ignoreCase) { lowercase() }
                .ifLet(normalizeText) { filterNot { it == ' ' } }
        }

    }

    data class ModuleLang(
        val blockedMessage: Map<String, MessageData> = linkedMapOf(
            Pair("bad-keywords", "<ec>You have bad words in the text.".message)
        ),
    ): ConfigurationPart

}