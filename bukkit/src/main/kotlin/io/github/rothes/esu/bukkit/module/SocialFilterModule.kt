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

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie
import io.github.ranlee1.jpinyin.PinyinFormat
import io.github.ranlee1.jpinyin.PinyinHelper
import io.github.rothes.esu.bukkit.event.RawUserChatEvent
import io.github.rothes.esu.bukkit.event.RawUserEmoteEvent
import io.github.rothes.esu.bukkit.event.RawUserWhisperEvent
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ServerInfo
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
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils.plainText
import io.github.rothes.esu.core.util.extension.ifLet
import io.github.rothes.esu.lib.adventure.text.TextComponent
import io.github.rothes.esu.lib.adventure.text.TranslatableComponent
import io.github.rothes.esu.lib.adventure.text.event.HoverEvent
import io.github.rothes.esu.lib.adventure.text.format.TextDecoration
import it.unimi.dsi.fastutil.chars.Char2ReferenceOpenHashMap
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
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

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onDeath(e: PlayerDeathEvent) {
            if (!ServerInfo.isPaper) return
            val values = filters.configs.values
            if (!values.any { it.enabled && it.blockItemName }) return

            val server = e.deathMessage() ?: return
            if (server !is net.kyori.adventure.text.TranslatableComponent) return // Only support vanilla messages

            val msg = server.esu as TranslatableComponent
            for (argument in msg.arguments()) {
                val arg = argument.value()
                if (arg is TranslatableComponent && arg.key() == "chat.square_brackets") {
                    val argument = arg.arguments()[0]
                    val item = argument.value() as? TextComponent ?: continue
                    if (arg.hoverEvent()?.value() !is HoverEvent.ShowItem) continue // Ensure it's item
                    if (!item.hasDecoration(TextDecoration.ITALIC)) continue // Not a custom name

                    val component = item.children().firstOrNull() as? TextComponent ?: continue
                    val content = component.content()
                    if (values.any { it.enabled && it.blockItemName && it.contains(content) }) {
                        e.deathMessage(null)
                        return
                    }
                }
            }
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
        @Comment("Block custom item names that shows in chat. For example, by death message.")
        val blockItemName: Boolean = false,
        @Comment("Convert Chinese characters to Pinyin.")
        val convertPinyin: Boolean = false,
        @Comment("Ignore case on matching texts.")
        val ignoreCase: Boolean = true,
        @Comment("Normalize text to fix bypassing by characters like blank.")
        val normalizeText: Boolean = true,
        @Comment("""
            The maximum number of allowed skipped intermediate characters between keywords.
            0 to disable this, and can improve performance.
        """)
        val maxCharGap: Int = 0,
        @Comment("""
            The message to send when blocked by this file.
            This is the key in the 'blocked-message' map in lang files.
        """)
        val blockedMessageKey: String = "bad-keywords",
        val keywords: List<String> = listOf("A keyword to block", "Another keyword to block"),
    ) : ConfigurationPart {

        private val searcher by lazy { if (maxCharGap > 0) GappedSearcher() else TrieSearcher() }

        fun contains(text: String): Boolean {
            return searcher.contains(preprocessText(text))
        }

        fun messageBlocked(user: User) {
            if (blockedMessageKey.isEmpty()) return
            val message = user.langOrNull(lang) { blockedMessage[blockedMessageKey] } ?: blockedMessageKey.message
            user.message(message)
        }

        fun preprocessText(text: String): String {
            return text
                .ifLet(convertPinyin) { PinyinHelper.convertToPinyinString(this, " ", PinyinFormat.WITHOUT_TONE) }
                .ifLet(ignoreCase) { lowercase() }
                .ifLet(normalizeText) { filterNot { it == ' ' } }
        }

        private interface Searcher {
            fun contains(text: String): Boolean
        }

        private inner class TrieSearcher : Searcher {

            private val trie = AhoCorasickDoubleArrayTrie<Filter>().also { trie ->
                trie.build(keywords.map { preprocessText(it) }.associateWith { this@Filter })
            }

            override fun contains(text: String): Boolean {
                return trie.findFirst(text) != null
            }

        }

        private inner class GappedSearcher : Searcher {

            private val root = TrieNode(keywords.map { preprocessText(it) })

            override fun contains(text: String): Boolean {
                for (i in text.indices) {
                    if (dfs(root, text, i, 0)) {
                        return true
                    }
                }
                return false
            }

            private fun dfs(node: TrieNode, text: String, i: Int, gapCount: Int): Boolean {
                if (node.isEnd) return true
                if (i >= text.length) return false

                val nextNode = node.children?.get(text[i])
                if (nextNode != null) {
                    if (dfs(nextNode, text, i + 1, 0)) return true
                }

                if (gapCount < maxCharGap) {
                    if (dfs(node, text, i + 1, gapCount + 1)) return true
                }

                return false
            }

        }

        private class TrieNode(patterns: List<String>) {

            @JvmField val isEnd: Boolean = patterns.any { it.isEmpty() }
            @JvmField val children: Char2ReferenceOpenHashMap<TrieNode>? =
                if (patterns.isNotEmpty()) {
                    val notEmpty = patterns.filterNot { it.isEmpty() }
                    if (notEmpty.isNotEmpty())
                        Char2ReferenceOpenHashMap(
                            notEmpty
                                .groupBy { it.first() }
                                .mapValues { TrieNode(it.value.map { s -> s.substring(1) }) }
                        )
                    else null
                } else null

        }

    }

    data class ModuleLang(
        val blockedMessage: Map<String, MessageData> = linkedMapOf(
            Pair("bad-keywords", "<ec>You have bad words in the text.".message)
        ),
    ): ConfigurationPart

}