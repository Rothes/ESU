package io.github.rothes.esu.bukkit.module.news

import io.github.rothes.esu.bukkit.module.NewsModule
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.storage.StorageManager.database
import io.github.rothes.esu.core.storage.StorageManager.upgrader
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.DataSerializer.deserialize
import io.github.rothes.esu.core.util.DataSerializer.serialize
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json

object NewsDataManager {

    object NewsTable: Table("news_data") {
        val id = integer("id").autoIncrement()
        val channel = varchar("channel", 32).index("ix_channel")
        val time = datetime("time")
        val data = json<NewsItem>("data", { it.serialize() }, { it.deserialize() })

        override val primaryKey = PrimaryKey(id)
    }

    object NewsCheckedTable: Table("news_checked") {
        val user = integer("user").references(StorageManager.UsersTable.dbId, ReferenceOption.CASCADE, ReferenceOption.CASCADE, "fk_news_checked__user__id")
        val channel = varchar("channel", 32)
        val checked = integer("checked")

        override val primaryKey: PrimaryKey = PrimaryKey(user, channel)
    }

    private var task: ScheduledTask? = null
    var news: List<NewsItem> = emptyList()
        private set

    init {
        transaction(database) {
            SchemaUtils.create(NewsTable)
            SchemaUtils.create(NewsCheckedTable)
            NewsTable.upgrader({
                exec("ALTER TABLE `${NewsTable.tableName}` DROP INDEX `news_data_channel`")
                exec("ALTER TABLE `${NewsTable.tableName}` ADD INDEX `ix_channel` (channel)")
            })
            NewsCheckedTable.upgrader({
                exec("ALTER TABLE `${NewsCheckedTable.tableName}` DROP FOREIGN KEY `fk_news_checked_user__id`")
                exec("ALTER TABLE `${NewsCheckedTable.tableName}` ADD CONSTRAINT `fk_news_checked__user__id` FOREIGN KEY (`user`) REFERENCES `users` (`id`) ON UPDATE CASCADE ON DELETE CASCADE")
            })
        }
    }

    fun start() {
        task?.cancel()
        fetchNews()
        fun schedule() {
            // We want a random offset
            task = Scheduler.asyncTicks(20 * 60L + (-200 .. 200).random()) {
                fetchNews()
                schedule()
            }
        }
        schedule()
    }

    fun shutdown() {
        task?.cancel()
        task = null
    }

    fun onReload() {
        shutdown()
        start()
    }

    fun fetchNews(post: (() -> Unit)? = null) {
        StorageManager.coroutineScope.launch {
            transaction(database) {
                news = NewsTable.select(NewsTable.id, NewsTable.time, NewsTable.data)
                    .where { NewsTable.channel eq currentChannel }
                    .orderBy(NewsTable.time, SortOrder.DESC)
                    .map {
                        it[NewsTable.data].copy(time = it[NewsTable.time], id = it[NewsTable.id])
                    }
            }
            post?.invoke()
        }
    }

    fun addNews(news: NewsItem, post: (() -> Unit)? = null) {
        StorageManager.coroutineScope.launch {
            transaction(database) {
                NewsTable.insert {
                    it[channel] = currentChannel
                    it[time] = news.time
                    it[data] = news
                }
            }
            fetchNews(post)
        }
    }

    fun updateNews(news: NewsItem, post: (() -> Unit)? = null) {
        require(news.id >= 0) { "News id must be non-negative." }
        StorageManager.coroutineScope.launch {
            transaction(database) {
                NewsTable.update( { NewsTable.id eq news.id } ) {
                    it[time] = news.time
                    it[data] = news
                }
            }
            fetchNews(post)
        }
    }

    fun deleteNews(news: NewsItem, post: (() -> Unit)? = null) {
        StorageManager.coroutineScope.launch {
            transaction(database) {
                NewsTable.deleteWhere { NewsTable.id eq news.id }
            }
            fetchNews(post)
        }
    }

    fun getChecked(user: User): Int {
        return transaction(database) {
            NewsCheckedTable.select(NewsCheckedTable.checked).where {
                NewsCheckedTable.user eq user.dbId and (NewsCheckedTable.channel eq currentChannel)
            }.firstOrNull()?.get(NewsCheckedTable.checked) ?: -1
        }
    }

    fun setChecked(user: User, checked: Int) {
        StorageManager.coroutineScope.launch {
            transaction(database) {
                with(NewsCheckedTable) {
                    val lines = update({ NewsCheckedTable.user eq user.dbId and (channel eq currentChannel) }) {
                        it[NewsCheckedTable.checked] = checked
                    }
                    if (lines == 0)
                        insert {
                            it[NewsCheckedTable.user] = user.dbId
                            it[NewsCheckedTable.channel] = currentChannel
                            it[NewsCheckedTable.checked] = checked
                        }
                }
//                NewsCheckedTable.upsert(NewsCheckedTable.user, NewsCheckedTable.channel) {
//                    it[NewsCheckedTable.user] = user.dbId
//                    it[NewsCheckedTable.channel] = currentChannel
//                    it[NewsCheckedTable.checked] = checked
//                }
            }
        }
    }

    private val currentChannel
        get() = NewsModule.config.bookNews.channel


    data class NewsItem(
        val msg: Map<String, List<String>>,
        @Transient val time: LocalDateTime,
        @Transient val id: Int = -1,
    )

}