package io.github.rothes.esu.core.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.config.EsuLocale
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.net.URI
import java.util.*

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
    private val messages = mutableListOf<Map<String, String>>()
    private var errorCount = 0

    fun onJoin(user: User) {
        if (user.hasPermission(perm)) {
            for (msg in messages) {
                user.message(user.localed(msg))
            }
        }
    }

    fun run() {
        val info = fetch()
        for (message in info.errorMessage) {
            console.log(EsuLocale.get(), message.scope, *message.args)
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

            console.log(console.localed(notification.message))
            if (notification.notifyInGame) {
                allUsers()
                    .filter { it.hasPermission(perm) }
                    .forEach {
                        it.message(it.localed(notification.message))
                    }
            }

            messages.add(notification.message)
        }
    }

    fun fetch(): CheckedInfo {
        val fetch = getResponse("raw.githubusercontent.com")
        val errors = mutableListOf<LocaledMessage>()

        fun err(vararg arg: TagResolver, scope: EsuLocale.BaseEsuLocaleData.() -> String?) {
            errors.add(LocaledMessage(*arg, scope = scope))
        }

        fetch.errorMessage?.let { errors.add(it) }
        val response = fetch.response ?: return CheckedInfo(null, errors)
        return CheckedInfo(
            buildList {
                messages.clear()
                response.versionChannel[channel]?.let { channel ->
                    channel[platform]?.let { info ->
                        if (versionId < info.latestVersionId) {
                            add(info.notification)
                        }
                    } ?: err(ComponentUtils.unparsed("platform", platform)) { updater.checker.unknownPlatform }
                } ?: err(ComponentUtils.unparsed("channel", channel)) { updater.checker.unknownChannel }

                response.versionAction[platform]?.let { map ->
                    for ((range, notification) in map) {
                        val versions = range.split('-').map { it.toInt() }
                        if (versionId in versions[0] .. versions[1]) {
                            add(notification)
                        }
                    }
                }
            },
            errors
        )
    }

    private fun User.localed(map: Map<String, String>): MessageData {
        val key = language
        val raw = map[key]
            // If this locale is not found, try the same language.
            ?: key?.split('_')?.get(0)?.let { language ->
                val lang = language + '_'
                map.entries.firstOrNull { it.key.startsWith(lang) }?.value
            }
            // Still? Use the server default locale instead.
            ?: map[EsuConfig.get().locale]
            // Use the default value.
            ?: map["en_us"]
            // Maybe it doesn't provide en_us locale...?
            ?: map.values.first()
        return MessageData(raw)
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
                return getResponse("ghfast.top/https://raw.githubusercontent.com", 1)
            }
            if (errorCount < 3) {
                errorCount++
                return FetchedResponse(errorMessage = LocaledMessage(ComponentUtils.unparsed("message", e)) {
                    updater.checker.networkError
                })
            }
            return FetchedResponse()
        }
    }

    private data class FetchedResponse(
        val response: Response? = null,
        val errorMessage: LocaledMessage? = null,
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
            val notification: Notification,
        )
    }

    data class CheckedInfo(
        val notifications: List<Notification>?,
        val errorMessage: List<LocaledMessage> = listOf(),
    )

    data class Notification(
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

    class LocaledMessage(
        vararg val args: TagResolver,
        val scope: EsuLocale.BaseEsuLocaleData.() -> String?,
    )

}