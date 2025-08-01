package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.config.BukkitEsuLocale
import io.github.rothes.esu.bukkit.config.data.ItemData
import io.github.rothes.esu.bukkit.event.UserLoginEvent
import io.github.rothes.esu.bukkit.module.news.EditorManager
import io.github.rothes.esu.bukkit.module.news.NewsDataManager
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.parsed
import io.github.rothes.esu.core.util.ComponentUtils.time
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.core.util.ConversionUtils.localDateTime
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.incendo.cloud.annotations.Command
import org.spongepowered.configurate.objectmapping.meta.Comment
import kotlin.jvm.java

object NewsModule: BukkitModule<NewsModule.ModuleConfig, NewsModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    private val checkedCache = mutableMapOf<User, Int>()

    override fun canUse(): Boolean {
        if (!super.canUse())
            return false
        if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
            plugin.err("[NewsModule] This module requires packetevents plugin!")
            return false
        }
        return true
    }

    override fun enable() {
        EditorManager.enable()
        NewsDataManager.start()
        registerCommands(Commands)
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
        Bukkit.getOnlinePlayers().map { it.user }.forEach {
            checkedCache[it] = NewsDataManager.getChecked(it)
        }
    }

    override fun disable() {
        super.disable()
        checkedCache.clear()
        HandlerList.unregisterAll(Listeners)
        EditorManager.disable()
        NewsDataManager.shutdown()
    }

    override fun reloadConfig() {
        super.reloadConfig()
        if (enabled)
            NewsDataManager.onReload()
    }

    private object Listeners: Listener {
        @EventHandler
        fun onJoin(e: UserLoginEvent) {
            val user = e.user
            val news = NewsDataManager.news
            if (news.isEmpty()) return
            val checked = NewsDataManager.getChecked(user)
            checkedCache[user] = checked
            if (config.bookNews.showUnreadNewsOnJoin && checked < news.first().id)
                Scheduler.schedule(user.player, 1) {
                    Commands.news(user)
                }
        }
        @EventHandler
        fun onQuit(e: PlayerQuitEvent) {
            checkedCache.remove(e.player.user)
        }
    }

    object Commands {
        @Command("news")
        fun news(user: User) {
            user as PlayerUser
            val news = NewsDataManager.news
            if (news.isEmpty()) {
                user.message(locale, { bookNews.noNewsSet })
                return
            }
            val page = user.localed(locale) { bookNews.layout }
            val check = component("check", user.buildMinimessage(locale, { bookNews.checkButton })
                .clickEvent(ClickEvent.runCommand("/news checked")))
            val checked = checkedCache[user] ?: -1
            user.openBook(Book.builder().pages(
                news.map { item ->
                    val content = user.localed(item.msg).joinToString("\n")
                    user.buildMinimessage(page, check,
                        component("content", user.buildMinimessage(content)),
                        component("new-placeholder",
                            if (item.id > checked)
                                user.buildMinimessage(locale, { bookNews.newPlaceholder })
                            else
                                Component.empty()
                        ))
                }
            ).build())
        }
        @Command("news checked")
        fun newsChecked(user: User) {
            user.message(locale, { bookNews.checked })
            val checked = NewsDataManager.news.firstOrNull()?.id ?: -1
            NewsDataManager.setChecked(user, checked)
            checkedCache[user] = checked
        }

        @ShortPerm("editor")
        @Command("news editor")
        fun editor(user: User) {
            user as PlayerUser
            val news = NewsDataManager.news
            val new = component("new", user.buildMinimessage(locale, { bookNews.editor.bookLayout.button.new })
                .clickEvent(ClickEvent.runCommand("/news editor new")))
            user.openBook(Book.builder().pages(
                if (news.isEmpty()) {
                    listOf(user.buildMinimessage(locale, { bookNews.editor.bookLayout.emptyLayout }, new))
                } else {
                    val page = user.localed(locale) { bookNews.editor.bookLayout.pageLayout }
                    news.map { item ->
                        val content = user.localed(item.msg).joinToString("\n")
                        user.buildMinimessage(page, new,
                            unparsed("id", item.id),
                            component("edit", user.buildMinimessage(locale, { bookNews.editor.bookLayout.button.edit })
                                .clickEvent(ClickEvent.runCommand("/news editor edit ${item.id}"))),
                            component("delete", user.buildMinimessage(locale, { bookNews.editor.bookLayout.button.delete })
                                .clickEvent(ClickEvent.runCommand("/news editor delete ${item.id}"))),
                            component("content", user.buildMinimessage(content)))
                    }
                }
            ).build())
        }

        @ShortPerm("editor")
        @Command("news editor new [lang]")
        fun new(user: User, lang: String = EsuConfig.get().locale) {
            user as PlayerUser
            val player = user.player
            val content = config.bookNews.newLayout.values.firstOrNull()?.initLayout(user) ?: ""
            val editorItem = user.item(locale, { bookNews.editor.editItem.copy(material = Material.WRITABLE_BOOK) })
            user.message(locale, { bookNews.editor.editStart })
            EditorManager.startEdit(player, content, -1, lang, editorItem, {
                user.message(locale, { bookNews.editor.editCancelled })
            }, { result ->
                val lang = result.lang
                val item = NewsDataManager.NewsItem(mapOf(lang to result.content), result.time.localDateTime, result.newsId)
                EditorManager.toConfirm(player) {
                    NewsDataManager.addNews(item) {
                        editor(user)
                    }
                }
                user.preview(item, lang)
            })
        }

        @ShortPerm("editor")
        @Command("news editor edit <id> [lang]")
        fun edit(user: User, id: Int, lang: String = EsuConfig.get().locale) {
            user as PlayerUser
            val player = user.player

            val item = NewsDataManager.news.find { it.id == id }
            if (item == null) {
                user.message(locale, { bookNews.editor.unknownNewsId }, unparsed("id", id))
                return
            }

            val content = item.msg[lang] ?: listOf(config.bookNews.newLayout.values.firstOrNull()?.initLayout(user) ?: "")
            val editorItem = user.item(locale, { bookNews.editor.editItem.copy(material = Material.WRITABLE_BOOK) })
            user.message(locale, { bookNews.editor.editStart })
            EditorManager.startEdit(player, content, id, lang, editorItem, {
                user.message(locale, { bookNews.editor.editCancelled })
            }, { result ->
                val map = item.msg.toMutableMap()
                map[result.lang] = result.content
                val new = item.copy(msg = map)
                EditorManager.toConfirm(player) {
                    NewsDataManager.updateNews(new) {
                        editor(user)
                    }
                }
                user.preview(new, result.lang)
            })
        }

        @ShortPerm("editor")
        @Command("news editor changelang")
        fun changeLang(user: User) {
            user as PlayerUser
            val player = user.player
            val editing = EditorManager.getEditing(player)
            if (editing == null) {
                user.message(locale, { bookNews.editor.notEditing })
                return
            }
            val id = editing.newsId
            val item = NewsDataManager.news.find { it.id == id }

            val builder = Component.text()
            for (lang in BukkitEsuLocale.get().configs.keys) {
                builder.append(
                    user.buildMinimessage(locale, {
                        if (item != null && item.msg.containsKey(lang))
                            bookNews.editor.changeLang.existsLang
                        else
                            bookNews.editor.changeLang.notExistsLang
                    }, parsed("lang", lang))
                )
            }
            user.message(locale, { bookNews.editor.changeLang.format },
                unparsed("current-lang", editing.lang),
                component("languages", builder.build()))
        }
        @ShortPerm("editor")
        @Command("news editor changelang <lang>")
        fun changeLang(user: User, lang: String) {
            user as PlayerUser
            val player = user.player
            val editing = EditorManager.getEditing(player)
            if (editing == null) {
                user.message(locale, { bookNews.editor.notEditing })
                return
            }
            val id = editing.newsId
            val item = NewsDataManager.news.find { it.id == id }

            val content = item?.msg[lang] ?: listOf(config.bookNews.newLayout.values.firstOrNull()?.initLayout(user) ?: "")
            val editorItem = user.item(locale, { bookNews.editor.editItem.copy(material = Material.WRITABLE_BOOK) })
            EditorManager.startEdit(player, content, editing.newsId, lang, editorItem, editing.cancel, editing.complete)
            user.message(locale, { bookNews.editor.changeLang.changedLang }, unparsed("lang", lang))
        }

        @ShortPerm("editor")
        @Command("news editor confirm")
        fun editConfirm(user: User) {
            user as PlayerUser
            if (!EditorManager.confirm(user.player))
                user.message(locale, { bookNews.editor.nothingToConfirm })
        }

        @ShortPerm("editor")
        @Command("news editor cancel")
        fun editCancel(user: User) {
            user as PlayerUser
            if (!EditorManager.cancel(user.player))
                user.message(locale, { bookNews.editor.nothingToConfirm })
            else
                user.message(locale, { bookNews.editor.editCancelled })
        }

        @ShortPerm("editor")
        @Command("news editor delete <id>")
        fun delete(user: User, id: Int) {
            user.message(locale, { bookNews.editor.deleteNewsConfirm }, parsed("id", id))
        }

        @ShortPerm("editor")
        @Command("news editor delete <id> confirm")
        fun deleteConfirm(user: User, id: Int) {
            val item = NewsDataManager.news.find { it.id == id }
            if (item == null) {
                user.message(locale, { bookNews.editor.unknownNewsId }, unparsed("id", id))
                return
            }
            NewsDataManager.deleteNews(item) {
                user.message(locale, { bookNews.editor.deletedNews }, unparsed("id", id))
            }
        }

        private fun PlayerUser.preview(item: NewsDataManager.NewsItem, lang: String) {
            openBook(Book.builder().pages(
                buildMinimessage(localed(locale) { bookNews.editor.bookLayout.previewLayout },
                    unparsed("id", item.id),
                    unparsed("lang", lang),
                    component("confirm", buildMinimessage(locale, { bookNews.editor.bookLayout.button.confirm })
                        .clickEvent(ClickEvent.runCommand("/news editor confirm"))),
                    component("cancel", buildMinimessage(locale, { bookNews.editor.bookLayout.button.cancel })
                        .clickEvent(ClickEvent.runCommand("/news editor cancel"))),
                    component("content", buildMinimessage(item.msg[lang]!!.joinToString("\n"))))
            ))
        }

        private fun String.initLayout(user: User): String {
            val resolver = user.colorSchemeTagResolver
            val build = MiniMessage.builder().editTags {
                it.resolver(TagResolver.standard()).resolver(time()).resolver(resolver)
            }.build()
            return build.serialize(build.deserialize(this))
        }
    }

    data class ModuleConfig(
        val bookNews: BookNews = BookNews(),
    ): BaseModuleConfiguration() {

        data class BookNews(
            @field:Comment("""
The news channel of this server.
All news data are stored in database, so if you have multiple
 same servers, you can use the same channel.""")
            val channel: String = "main",
            val showUnreadNewsOnJoin: Boolean = true,
            @field:Comment("The default layout when you create a new news.")
            val newLayout: Map<String, String> = mapOf(
                "default" to """
                    <pdc><b>Title </b> 
                    <pc>Body
                    
                    <tc><time:'yyyy-MM-dd HH:mm'>
                """.trimIndent()
            )
        )
    }

    data class ModuleLang(
        val bookNews: BookNews = BookNews(),
    ): ConfigurationPart {

        data class BookNews(
            val noNewsSet: MessageData = "<ec>This server has no news set.".message,
            val layout: String = """
                <pdc><shadow:black>Server News <check>
                <new-placeholder><reset><content>
            """.trimIndent(),
            val checkButton: String = "<vpdc>[Check]",
            val newPlaceholder: String = "<dark_green><bold>NEW!<br>",
            val checked: MessageData = "<pc>You have mark the news as checked. We won't notify you again until there's something new.".message,
            val editor: Editor = Editor(),
        ) {
            data class Editor(
                val bookLayout: BookLayout = BookLayout(),
                val changeLang: ChangeLang = ChangeLang(),
                val editItem: ItemData = ItemData(displayName = "<!i><pdc><b>Editor </b><tc>- <pc>right click to open"),
                val editStart: MessageData = "<pc>Right-click with the book item and start the edit.<chat><pc>Click <lang:gui.done> when you are done.<chat><sc><click:run_command:'/news editor changelang'>Click here if you want to change the lang editing.".message,
                val editCancelled: MessageData = "<sc>You have cancelled the edit.".message,
                val notEditing: MessageData = "<ec>You are not editing any news.".message,
                val nothingToConfirm: MessageData = "<ec>You have nothing to confirm.".message,
                val unknownNewsId: MessageData = "<ec>Unknown news with ID <edc><id></edc>, operate again in gui?".message,
                val deleteNewsConfirm: MessageData = "<ec>Are you sure to delete the news <id>? <click:run_command:'/news editor delete <id> confirm'><edc>[Confirm]".message,
                val deletedNews: MessageData = "<pc>Deleted the news <pdc><id></pdc>.".message,
            ) {
                data class ChangeLang(
                    val format: MessageData = "<pc>Current editing lang: <pdc><current-lang><chat><pc>Select the lang: <languages>".message,
                    val existsLang: String = "<vpc><click:run_command:'/news editor changelang <lang>'><hover:show_text:'<pc>Click to edit <lang>'><lang><reset> ",
                    val notExistsLang: String = "<vnc><click:run_command:'/news editor changelang <lang>'><hover:show_text:'<pc>Click to edit <lang>'><lang><reset> ",
                    val changedLang: MessageData = "<pc>Changed the lang to <pdc><lang>".message,
                )
                data class BookLayout(
                    val emptyLayout: String = "<pc>There's no any news yet.\n\n<new>",
                    val pageLayout: String = """
                        <pdc><id> <new> <edit> <delete>
                        <reset><content>
                    """.trimIndent(),
                    val previewLayout: String = """
                        <pdc><b>Preview</b> <id> <pc><lang>
                          <confirm>   <cancel>
                        <reset><content>
                    """.trimIndent(),
                    val button: Button = Button(),
                ) {
                    data class Button(
                        val new: String = "<pdc>[New]",
                        val edit: String = "<pdc>[Edit]",
                        val delete: String = "<vndc>[Del]",
                        val confirm: String = "<vpdc>[Confirm]",
                        val cancel: String = "<vndc>[Cancel]",
                    )
                }
            }
        }
    }

}