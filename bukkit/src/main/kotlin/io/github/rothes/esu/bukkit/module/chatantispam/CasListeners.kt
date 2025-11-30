package io.github.rothes.esu.bukkit.module.chatantispam

import io.github.rothes.esu.bukkit.event.RawUserChatEvent
import io.github.rothes.esu.bukkit.event.RawUserEmoteEvent
import io.github.rothes.esu.bukkit.event.RawUserWhisperEvent
import io.github.rothes.esu.bukkit.event.UserLoginEvent
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.config
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.hasPerm
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.lang
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.msgPrefix
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.spamData
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageMeta
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import io.github.rothes.esu.bukkit.module.chatantispam.user.CasDataManager
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.adventure.text.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

object CasListeners: Listener {

    val notifyUsers = hashSetOf<User>()

    fun enable() {
        CasListeners.register()
    }

    fun disable() {
        CasListeners.unregister()
    }

    @EventHandler
    fun onChat(e: RawUserChatEvent) {
        if (checkBlocked(e.player, e.message.legacy, Chat)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onEmote(e: RawUserEmoteEvent) {
        if (checkBlocked(e.player, e.message, Emote)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onWhisper(e: RawUserWhisperEvent) {
        if (checkBlocked(e.player, e.message, Whisper(e.target))) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val server = event.deathMessage() ?: return
        val deathMessage = server.esu
        if (deathMessage is TranslatableComponent) {
            if (checkBlocked(event.player, deathMessage.legacy, Death)) {
                if (ServerCompatibility.isPaper && ServerCompatibility.serverVersion >= "1.21.5") {
                    event.deathScreenMessageOverride(server)
                }
                event.deathMessage(null)
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun checkBlocked(player: Player, message: String, messageMeta: MessageMeta): Boolean {
        val user = player.user
        if (user.hasPerm("bypass")) {
            return false
        }
        val spamData = user.spamData.purge()
        val now = System.currentTimeMillis()
        spamData.requests.sizedAdd(config.expireSize.chatRequest, now)

        val spamCheck = config.spamCheck.getOrDefault(messageMeta.type) ?: return false
        val request = MessageRequest(player, messageMeta, spamCheck, spamData, now, message)

        var blockValue = false
        var scoreValue = 0.0
        var scoreBy = "pass"
        var mergeScoreValue = false
        var muted = false
        for (check in ChecksMan.checks) {
            val (filter, score, mergeScore, notify, addFilter, mute, block, endChecks) = check.check(request)
            if (filter != null) {
                val shouldNotify = notify ?: spamCheck.notifyFiltered
                handleFiltered(player, message, messageMeta, filter, spamData, shouldNotify, addFilter)
            }
            if (mute) {
                handleMuted(player, spamData)
                muted = true
            }
            if (scoreValue < 0 || score < 0) {
                scoreValue = min(scoreValue, score)
            } else if (score > scoreValue) {
                scoreValue = score
                mergeScoreValue = mergeScore
                scoreBy = filter ?: scoreBy
            }
            if (block) {
                blockValue = true
            }
            if (endChecks) {
                break
            }
        }
        if (messageMeta.type != MessageType.DEATH && scoreValue >= 0) {
            // A lucky player can get muted in check and in this! This is intentional.
            var remainScore = scoreValue
            if (mergeScoreValue) {
                spamData.scores.lastOrNull()?.let {
                    if (it.type == scoreBy) {
                        val toAdd = min(1.0 - it.score, remainScore)
                        it.score += toAdd
                        it.time = now
                        if (remainScore > toAdd) {
                            remainScore -= toAdd
                        } else {
                            remainScore = -1.0
                        }
                    }
                }
            }
            if (remainScore >= 0) {
                spamData.scores.sizedAdd(config.expireSize.score, SpamData.SpamScore(scoreBy, remainScore, now))
            }
            val spamScoreConfig = config.muteHandler.spamScore
            if (spamScoreConfig.muteOnAverageScore > 0) {
                val scores = spamData.scores.takeLast(spamScoreConfig.calculateSize)
                val sum = scores.sumOf { it.score }
                val max = spamScoreConfig.muteOnAverageScore * spamScoreConfig.calculateSize
                if (sum >= max) {
                    val safeCheck = scores.drop(1)
                    var toRemove = safeCheck.sumOf { it.score } - (max - spamScoreConfig.safeScoreOnMute)
                    var i = 0
                    while (toRemove > 0 && i < safeCheck.size) {
                        val score = safeCheck[i]
                        if (toRemove > score.score) {
                            score.score = 0.0
                            toRemove -= score.score
                        } else {
                            score.score -= toRemove
                            toRemove = 0.0
                        }
                        i++
                    }
                    handleFiltered(player, if (blockValue) "§8<same above>" else message, messageMeta, "score",
                        spamData,
                        notify = true,
                        addFilter = false
                    )
                    handleMuted(player, spamData)
                    muted = true
                    blockValue = true
                }
            }
        }
        if (!blockValue) {
            if (config.consecutiveUnfilteredThreshold > 0 && spamData.filtered.isNotEmpty()) {
                if (spamData.consecutiveUnfiltered.addAndFetch(1) >= config.consecutiveUnfilteredThreshold) {
                    spamData.filtered.removeFirst()
                    spamData.consecutiveUnfiltered.store(0)
                }
            }
            spamData.records.sizedAdd(config.expireSize.messageRecord, SpamData.MessageRecord(request.message, now))
        } else {
            spamData.consecutiveUnfiltered.store(0)
        }
        if (muted)
            CasDataManager.saveSpamDataAsync(user)
        return blockValue
    }

    private fun handleFiltered(player: Player, message: String, messageMeta: MessageMeta, checkType: String, spamData: SpamData, notify: Boolean, addFilter: Boolean): Boolean {
        if (notify) {
            notifyUsers.forEach {
                it.message(lang, { this.notify.filtered },
                    player(player), it.msgPrefix,
                    unparsed("message", message), unparsed("check-type", checkType), unparsed("chat-type", messageMeta)
                )
            }
        }
        if (!addFilter) {
            return true
        }
        spamData.purge()
        val now = System.currentTimeMillis()
        spamData.filtered.sizedAdd(config.expireSize.filtered, now)
        if (spamData.filtered.size >= config.muteHandler.muteOnFilteredSize) {
            handleMuted(player, spamData)
        }
        return true
    }

    private fun handleMuted(player: Player, spamData: SpamData) {
        val duration = spamData.mute()
        notifyUsers.forEach {
            it.message(lang, { this.notify.muted },
                player(player), it.msgPrefix,
                duration(duration.milliseconds, it),
                unparsed("multiplier", String.format("%.1f", spamData.muteMultiplier)),
            )
        }
    }

    fun <T> ArrayDeque<T>.sizedAdd(fullOn: Int, obj: T): ArrayDeque<T> {
        synchronized(this) {
            dropIfFull(fullOn)
            add(obj)
        }
        return this
    }

    private fun <T> ArrayDeque<T>.dropIfFull(fullOn: Int): ArrayDeque<T> {
        if (size == fullOn) {
            this.removeFirst()
        }
        return this
    }

    object Chat: MessageMeta(MessageType.CHAT, null)
    object Death: MessageMeta(MessageType.DEATH, null)
    object Emote: MessageMeta(MessageType.EMOTE, null)
    class Whisper(receiver: String): MessageMeta(MessageType.WHISPER, receiver)

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val playerUser = event.player.user
        CasDataManager.loadSpamData(playerUser)
    }

    @EventHandler
    fun onJoin(event: UserLoginEvent) {
        val user = event.user
        if (user.hasPerm("notify"))
            notifyUsers.add(user)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val playerUser = event.player.user
        if (playerUser.logonBefore)
            CasDataManager.saveSpamDataAsync(playerUser)
        notifyUsers.remove(playerUser)
    }

}