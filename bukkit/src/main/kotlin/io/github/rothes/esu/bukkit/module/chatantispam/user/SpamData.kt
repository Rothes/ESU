package io.github.rothes.esu.bukkit.module.chatantispam.user

import com.google.gson.annotations.SerializedName
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule.config
import io.github.rothes.esu.core.util.CollectionUtils.removeWhile
import io.github.rothes.esu.core.util.extension.DurationExt.compareTo
import io.github.rothes.esu.core.util.extension.DurationExt.valuePositive
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.min

data class SpamData(
    @SerializedName("t")
    var muteUntil: Long = -1,
    @SerializedName("m")
    var muteMultiplier: Double = 1.0,
    /**
     * Sent messages.
     */
    @SerializedName("rc")
    val records: ArrayDeque<MessageRecord> = ArrayDeque(),
    @SerializedName("sc")
    val scores: ArrayDeque<SpamScore> = ArrayDeque(),
    /**
     * Other player names tried to whisper.
     */
    @SerializedName("wt")
    val whisperTargets: ArrayDeque<WhisperTarget> = ArrayDeque(),
    /**
     * All timestamps that the user tried to send a message, whatever filtered.
     */
    @SerializedName("rq")
    val requests: ArrayDeque<Long> = ArrayDeque(),
    /**
     * All timestamps that the message filtered sent.
     */
    @SerializedName("ft")
    val filtered: ArrayDeque<Long> = ArrayDeque(),
) {

    @OptIn(ExperimentalAtomicApi::class)
    @Transient var consecutiveUnfiltered: AtomicInt = AtomicInt(0)
    @Transient var lastAccess: Long = -1

    fun mute(): Long {
        val now = System.currentTimeMillis()
        with(config.muteHandler.muteDurationMultiplier) {
            muteMultiplier = if (now - muteUntil <= maxMuteInterval) min(muteMultiplier * multiplier, multiplierMax) else 1.0
        }

        val currMute = if (muteUntil > now) muteUntil - now else 0
        muteUntil = (now + config.muteHandler.baseMuteDuration.toMillis() * muteMultiplier).toLong()
        lastAccess = muteUntil
        val muteDuration = muteUntil - now
        if (config.muteHandler.keepMessageRecords) {
            // We don't want it to keep forever if someone is getting muted while already muted
            records.forEach { it.time = it.time + muteDuration - currMute }
        }
        if (config.muteHandler.keepScores) {
            scores.forEach { it.time = it.time + muteDuration - currMute }
        }
        return muteDuration
    }

    fun purge(afkTime: Long): SpamData {
        val now = System.currentTimeMillis()
        records.removeWhile { config.expireTime.messageRecord.expired(now - it.time, afkTime) }
        if (config.expireTime.whisperTarget.valuePositive)
            whisperTargets.removeIf { now - it.lastTime > config.expireTime.whisperTarget.toMillis() }
        if (config.expireTime.chatRequest.valuePositive)
            requests.removeWhile { now - it > config.expireTime.chatRequest.toMillis() }
        if (config.expireTime.filtered.valuePositive)
            filtered.removeWhile { now - it > config.expireTime.filtered.toMillis() }
        if (config.expireTime.score > 0)
            scores.removeWhile { now - it.time > config.expireTime.score }
        return this
    }

    data class MessageRecord(
        @SerializedName("m")
        val message: String,
        @SerializedName("t")
        var time: Long,
    )

    data class SpamScore(
        @SerializedName("k")
        val type: String,
        @SerializedName("s")
        var score: Double,
        @SerializedName("t")
        var time: Long
    )

    data class WhisperTarget(
        @SerializedName("p")
        val player: String,
        @SerializedName("t")
        var lastTime: Long,
    )
}