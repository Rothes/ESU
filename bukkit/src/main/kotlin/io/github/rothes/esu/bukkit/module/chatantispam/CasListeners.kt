package io.github.rothes.esu.bukkit.module.chatantispam

import io.github.rothes.esu.bukkit.event.UserLoginEvent
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.config
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.hasPerm
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.locale
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.msgPrefix
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.spamData
import io.github.rothes.esu.bukkit.module.EsuChatModule
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageMeta
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import io.github.rothes.esu.bukkit.module.chatantispam.user.CasDataManager
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.net.kyori.adventure.text.TranslatableComponent
import io.github.rothes.esu.lib.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

object CasListeners: Listener {

    val notifyUsers = hashSetOf<User>()

    private val emoteCommands = EsuChatModule.EMOTE_COMMANDS.split('|').toSet()
    private val whisperCommands = EsuChatModule.WHISPER_COMMANDS.split('|').toSet()
    private val chatListener = try {
        AsyncChatEvent::class.java.toString()
        object : Listener {
            @EventHandler(priority = EventPriority.HIGH)
            fun onChat(event: AsyncChatEvent) {
                if (checkBlocked(event.player, LegacyComponentSerializer.legacySection().serialize(event.message().esu), Chat)) {
                    event.isCancelled = true
                }
            }
        }
    } catch (e: NoClassDefFoundError) {
        object : Listener {
            @EventHandler(priority = EventPriority.HIGH)
            fun onChat(event: AsyncPlayerChatEvent) {
                if (checkBlocked(event.player, event.message, Chat)) {
                    event.isCancelled = true
                }
            }
        }
    }

    fun enable() {
        Bukkit.getPluginManager().registerEvents(CasListeners, plugin)
        Bukkit.getPluginManager().registerEvents(chatListener, plugin)
    }

    fun disable() {
        HandlerList.unregisterAll(CasListeners)
        HandlerList.unregisterAll(chatListener)
    }

    @EventHandler
    fun onChatCommand(event: PlayerCommandPreprocessEvent) {
        val message = event.message
        val split = message.split(' ', limit = 3)
        val command = split[0].substring(1).split(':').last().lowercase()
        if (emoteCommands.contains(command)) {
            if (split.size >= 2 && checkBlocked(event.player, split.drop(1).joinToString(separator = " "), Emote)) {
                event.isCancelled = true
            }
        } else if (whisperCommands.contains(command)) {
            if (split.size >= 3 && checkBlocked(event.player, split[2], Whisper(split[1]))) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val deathMessage = event.deathMessage()
        if (deathMessage is TranslatableComponent) {
            if (checkBlocked(event.player, LegacyComponentSerializer.legacySection().serialize(deathMessage), Death)) {
                return event.deathMessage(null)
            }
        }
    }

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
        for (check in ChecksMan.checks) {
            val (filter, score, mergeScore, notify, addFilter, mute, block, endChecks) = check.check(request)
            if (filter != null) {
                val shouldNotify = notify ?: spamCheck.notifyFiltered
                handleFiltered(player, message, messageMeta, filter, spamData, shouldNotify, addFilter)
            }
            if (mute) {
                handleMuted(player, spamData)
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
                    blockValue = true
                }
            }
        }
        if (!blockValue) {
            spamData.records.sizedAdd(config.expireSize.messageRecord, SpamData.MessageRecord(request.message, now))
        }
        return blockValue
    }

    private fun handleFiltered(player: Player, message: String, messageMeta: MessageMeta, checkType: String, spamData: SpamData, notify: Boolean, addFilter: Boolean): Boolean {
        if (notify) {
            notifyUsers.forEach {
                it.message(locale, { this.notify.filtered },
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
            it.message(locale, { this.notify.muted },
                player(player), it.msgPrefix,
                duration(duration.milliseconds, it),
                unparsed("multiplier", String.format("%.1f", spamData.muteMultiplier)),
            )
        }
        CasDataManager.saveSpamDataAsync(player.user)
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