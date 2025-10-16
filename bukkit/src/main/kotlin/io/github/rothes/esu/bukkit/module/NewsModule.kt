package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.config.BukkitEsuLocale
import io.github.rothes.esu.bukkit.config.data.ItemData
import io.github.rothes.esu.bukkit.event.UserLoginEvent
import io.github.rothes.esu.bukkit.module.news.EditorManager
import io.github.rothes.esu.bukkit.module.news.NewsDataManager
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.parsed
import io.github.rothes.esu.core.util.ComponentUtils.time
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.core.util.ConversionUtils.localDateTime
import io.github.rothes.esu.lib.net.kyori.adventure.inventory.Book
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import io.github.rothes.esu.lib.net.kyori.adventure.text.event.ClickEvent
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.MiniMessage
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.incendo.cloud.annotations.Command

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
        Listeners.register()
        Bukkit.getOnlinePlayers().map { it.user }.forEach {
            checkedCache[it] = NewsDataManager.getChecked(it)
        }
    }

    override fun disable() {
        super.disable()
        checkedCache.clear()
        Listeners.unregister()
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

    fun User.context(
        tags: NewsDataManager.NewsItem.() -> List<TagResolver> = { listOf() },
        layout: ModuleLang.() -> String = { bookNews.layout }
    ): InterfaceContext {
        val layout = localed(locale, layout)
        val check = component("check", buildMiniMessage(locale, { bookNews.checkButton })
            .clickEvent(ClickEvent.runCommand("/news checked")))
        val checked = checkedCache[this] ?: -1
        return InterfaceContext(layout, check, checked, tags)
    }

    fun NewsDataManager.NewsItem.toPages(user: User, interfaceContext: InterfaceContext = user.context()): List<Component> {
        val (layout, check, checked, tags) = interfaceContext

        val pages = buildList {
            val sb = StringBuilder()
            for (page in user.localed(msg)) {
                if (page.isEmpty()) {
                    add(sb.toString())
                    sb.clear()
                } else {
                    if (sb.isNotEmpty())
                        sb.append('\n')
                    sb.append(page)
                }
            }

            if (sb.isNotEmpty())
                add(sb.toString())
        }

        return pages.map { page ->
            user.buildMiniMessage(layout, check,
                component("content", user.buildMiniMessage(page)),
                component("new-placeholder",
                    if (id > checked)
                        user.buildMiniMessage(locale, { bookNews.newPlaceholder })
                    else
                        Component.empty()
                ),
                *tags().toTypedArray()
            )
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
            val context = user.context()
            user.openBook(Book.builder().pages(news.flatMap { it.toPages(user, context) }).build())
        }

        @Command("news checked")
        fun newsChecked(user: User) {
            val latest = NewsDataManager.news.firstOrNull()?.id ?: -1
            if (checkedCache[user] == latest) {
                user.message(locale, { bookNews.checkedNothing })
                return
            }
            user.message(locale, { bookNews.checked })
            NewsDataManager.setChecked(user, latest)
            checkedCache[user] = latest
        }

        @ShortPerm("editor")
        @Command("news editor")
        fun editor(user: User) {
            user as PlayerUser
            val news = NewsDataManager.news
            val new = component("new", user.buildMiniMessage(locale, { bookNews.editor.bookLayout.button.new })
                .clickEvent(ClickEvent.runCommand("/news editor new")))
            user.openBook(Book.builder().pages(
                if (news.isEmpty()) {
                    listOf(user.buildMiniMessage(locale, { bookNews.editor.bookLayout.emptyLayout }, new))
                } else {
                    val context = user.context(
                        {
                            listOf(
                                new,
                                unparsed("id", id),
                                component("edit", user.buildMiniMessage(locale, { bookNews.editor.bookLayout.button.edit })
                                    .clickEvent(ClickEvent.runCommand("/news editor edit $id"))),
                                component("delete", user.buildMiniMessage(locale, { bookNews.editor.bookLayout.button.delete })
                                    .clickEvent(ClickEvent.runCommand("/news editor delete $id")))
                            )
                        }
                    ) { bookNews.editor.bookLayout.pageLayout }
                    news.flatMap { it.toPages(user, context) }
                }
            ).build())
        }

        @ShortPerm("editor")
        @Command("news editor new [lang]")
        fun new(user: User, lang: String = EsuConfig.get().locale) {
            user as PlayerUser
            val player = user.player
            val content = config.bookNews.newLayout.values.firstOrNull()?.initLayout(user) ?: ""
            user.message(locale, { bookNews.editor.editStart })
            EditorManager.startEdit(user, content, -1, lang, {
                user.message(locale, { bookNews.editor.editCancelled })
            }, { result ->
                val lang = result.editData.lang
                val item = NewsDataManager.NewsItem(mapOf(lang to result.content), result.editData.time.localDateTime, result.editData.newsId)
                EditorManager.toConfirm(player, result) {
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
            user.message(locale, { bookNews.editor.editStart })
            EditorManager.startEdit(user, content, id, lang, {
                user.message(locale, { bookNews.editor.editCancelled })
            }, { result ->
                val map = item.msg.toMutableMap()
                map[result.editData.lang] = result.content
                val new = item.copy(msg = map)
                EditorManager.toConfirm(player, result) {
                    NewsDataManager.updateNews(new) {
                        editor(user)
                    }
                }
                user.preview(new, result.editData.lang)
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
                    user.buildMiniMessage(locale, {
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
            EditorManager.startEdit(user, content, editing.newsId, lang, editing.cancel, editing.complete)
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
        @Command("news editor editAgain")
        fun editAgain(user: User) {
            user as PlayerUser
            EditorManager.editAgain(user.player)
        }

        @ShortPerm("editor")
        @Command("news editor reEdit")
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
            val context = context(
                {
                    listOf(
                        unparsed("id", item.id),
                        unparsed("lang", lang),
                        component("confirm", buildMiniMessage(locale, { bookNews.editor.bookLayout.button.confirm })
                            .clickEvent(ClickEvent.runCommand("/news editor confirm"))),
                        component("cancel", buildMiniMessage(locale, { bookNews.editor.bookLayout.button.cancel })
                            .clickEvent(ClickEvent.runCommand("/news editor cancel"))),
                        component("edit", buildMiniMessage(locale, { bookNews.editor.bookLayout.button.edit })
                            .clickEvent(ClickEvent.runCommand("/news editor editAgain"))),
                    )
                }
            ) { bookNews.editor.bookLayout.previewLayout }
            openBook(Book.builder().pages(item.toPages(this, context)))
        }

        private fun String.initLayout(user: User): String {
            val resolver = user.colorSchemeTagResolver
            val build = MiniMessage.builder().editTags {
                it.resolver(TagResolver.standard()).resolver(time()).resolver(resolver)
            }.build()
            return build.serialize(build.deserialize(this))
        }
    }

    data class InterfaceContext(
        val layout: String,
        val check: TagResolver,
        val checked: Int,
        val tags: NewsDataManager.NewsItem.() -> List<TagResolver>
    )

    data class ModuleConfig(
        val bookNews: BookNews = BookNews(),
    ): BaseModuleConfiguration() {

        data class BookNews(
            @Comment("""
                The news channel of this server.
                All news data are stored in database, so if you have multiple
                 same servers, you can use the same channel.
            """)
            val channel: String = "main",
            val showUnreadNewsOnJoin: Boolean = true,
            @Comment("The default layout when you create a new news.")
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
            val checkedNothing: MessageData = "<ec>You have nothing to check.".message,
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
                         <confirm> <edit> <cancel>
                        <reset><content>
                    """.trimIndent(),
                    val button: Button = Button(),
                ) {
                    data class Button(
                        val new: String = "<pdc>[New]",
                        val edit: String = "<pdc>[Edit]",
                        val delete: String = "<vndc>[Del]",
                        val confirm: String = "<vpdc>[Save]",
                        val cancel: String = "<vndc>[Cancel]",
                    )
                }
            }
        }
    }

}