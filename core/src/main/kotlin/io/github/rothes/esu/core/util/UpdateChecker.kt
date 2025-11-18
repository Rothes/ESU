package io.github.rothes.esu.core.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.config.EsuLang
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.coroutine.AsyncScope
import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URI
import java.util.*
import kotlin.time.Duration.Companion.hours

private const val GITHUB_REPO = "Rothes/ESU"

class UpdateChecker(
    private val versionId: Int,
    private val channel: String,
    private val platform: String,
    private val console: LogUser,
    private val actionScope: EnumMap<VersionAction, () -> Unit>,
    private val allUsers: () -> Iterable<User>,
    premRoot: String,
) {

    private val perm = "$premRoot.updater.receive"

    private val messageTimesMap = mutableMapOf<String, Int>()
    private val messages = mutableListOf<RemoteMessage>()
    private var errorCount = 0

    private var task: Job? = null

    init {
        onReload()
    }

    fun onReload() {
        if (EsuConfig.get().updateChecker) {
            if (task == null) {
                task = AsyncScope.launch {
                    while (isActive) {
                        run()
                        delay(1.hours)
                    }
                }
            }
        } else {
            shutdown()
        }
    }

    fun onJoin(user: User) {
        if (EsuConfig.get().updateChecker && user.hasPermission(perm)) {
            for (msg in messages) {
                user.message(user.localed(msg.langMap) { it.message }, *msg.args)
            }
        }
    }

    fun shutdown() {
        task?.cancel()
        task = null
    }

    fun run() {
        val info = fetch()
        for (message in info.errorMessage) {
            console.log(EsuLang.get(), message.scope, *message.args)
        }
        if (info.notifications == null)
            return

        messages.clear()
        for (notification in info.notifications) {
            for (action in notification.actions) {
                actionScope[action]?.invoke() ?: EsuCore.instance.warn("Unknown update checker action $action")
            }

            if (notification.messageTimes > 0) {
                val key = notification.message.values.first()
                val curr = messageTimesMap.getOrDefault(key, 0)
                if (curr < notification.messageTimes) {
                    messageTimesMap[key] = curr + 1
                } else {
                    continue
                }
            }

            val args = arrayOf(unparsed("latest_version", info.latestVersionName))
            val msg = RemoteMessage(notification.message, *args)

            console.log(console.localed(msg.langMap) { it.message }, *msg.args)
            if (notification.notifyInGame) {
                allUsers()
                    .filter { it.hasPermission(perm) }
                    .forEach { user ->
                        user.message(user.localed(msg.langMap) { it.message }, *msg.args)
                    }
            }

            messages.add(msg)
        }
    }

    private fun fetch(): CheckedInfo {
        val fetch = getResponse("ghfast.top/https://raw.githubusercontent.com")
        val errors = mutableListOf<LocaleMessage>()

        fun err(vararg arg: TagResolver, scope: EsuLang.BaseEsuLangData.() -> String?) {
            errors.add(LocaleMessage(*arg, scope = scope))
        }

        fetch.errorMessage?.let { errors.add(it) }
        val response = fetch.response ?: return CheckedInfo(null, "unknown", errors)
        return CheckedInfo(
            buildList {
                messages.clear()
                response.versionChannel[channel]?.let { channel ->
                    channel[platform]?.let { info ->
                        if (versionId < info.latestVersionId) {
                            add(info.notification)
                        }
                    } ?: err(unparsed("platform", platform)) { updater.checker.unknownPlatform }
                } ?: err(unparsed("channel", channel)) { updater.checker.unknownChannel }

                response.versionAction[platform]?.let { map ->
                    for ((range, notification) in map) {
                        val versions = range.split('-').map { it.toInt() }
                        if (versionId in versions[0] .. versions[1]) {
                            add(notification)
                        }
                    }
                }
            },
            response.versionChannel[channel]?.get(platform)?.latestVersionName ?: "unknown",
            errors
        )
    }

    private fun getResponse(domain: String, tryTimes: Int = 0): FetchedResponse {
        try {
            URI("https://$domain/$GITHUB_REPO/master/Updater_Data.json").toURL().openStream().bufferedReader()
                .use { reader ->
                    val json = reader.readText()
                    return FetchedResponse(Gson().fromJson(json, Response::class.java)).also {
                        errorCount = 0
                    }
                }
        } catch (e: Throwable) {
            if (tryTimes == 0) {
                return getResponse("raw.githubusercontent.com", 1)
            }
            if (errorCount < 3) {
                errorCount++
                return FetchedResponse(errorMessage = LocaleMessage(unparsed("message", e)) {
                    updater.checker.networkError
                })
            }
            return FetchedResponse()
        }
    }

    private data class FetchedResponse(
        val response: Response? = null,
        val errorMessage: LocaleMessage? = null,
    )


    private data class Response(
        @SerializedName("version_channel")
        val versionChannel: Map<String, Map<String, VersionInfo>>,
        @SerializedName("version_action")
        val versionAction: Map<String, Map<String, Notification>>,
    ) {
        data class VersionInfo(
            @SerializedName("latest_version_id")
            val latestVersionId: Int,
            @SerializedName("latest_version_name")
            val latestVersionName: String,
            val notification: Notification,
        )
    }

    private data class CheckedInfo(
        val notifications: List<Notification>?,
        val latestVersionName: String,
        val errorMessage: List<LocaleMessage> = listOf(),
    )

    private data class Notification(
        val actions: List<VersionAction>,
        @SerializedName("message_times")
        val messageTimes: Int,
        @SerializedName("notify_in_game")
        val notifyInGame: Boolean,
        val message: Map<String, String>,
    )

    enum class VersionAction {
        PROHIBIT
    }

    class LocaleMessage(
        vararg val args: TagResolver,
        val scope: EsuLang.BaseEsuLangData.() -> String?,
    )

    class RemoteMessage(
        val langMap: Map<String, String>,
        vararg val args: TagResolver,
    )

}