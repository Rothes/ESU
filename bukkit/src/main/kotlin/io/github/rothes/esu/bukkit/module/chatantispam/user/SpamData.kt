package io.github.rothes.esu.bukkit.module.chatantispam.user

import com.google.gson.annotations.SerializedName
import io.github.rothes.esu.bukkit.module.chatantispam.ChatAntiSpamModule.config
import kotlin.math.max

data class SpamData(
    @SerializedName("t", alternate = ["muteUntil"])
    var muteUntil: Long = -1,
    @SerializedName("m", alternate = ["muteMultiplier"])
    var muteMultiplier: Double = 1.0,
    /**
     * Sent messages.
     */
    @SerializedName("rc", alternate = ["records"])
    val records: ArrayDeque<MessageRecord> = ArrayDeque(),
    @SerializedName("sc", alternate = ["scores"])
    val scores: ArrayDeque<SpamScore> = ArrayDeque(),
    /**
     * Other player names tried to whisper.
     */
    @SerializedName("wt", alternate = ["whisperTargets"])
    val whisperTargets: ArrayDeque<WhisperTarget> = ArrayDeque(),
    /**
     * All timestamps that the user tried to send a message, whatever filtered.
     */
    @SerializedName("rq", alternate = ["requests"])
    val requests: ArrayDeque<Long> = ArrayDeque(),
    /**
     * All timestamps that the message filtered sent.
     */
    @SerializedName("ft", alternate = ["filtered"])
    val filtered: ArrayDeque<Long> = ArrayDeque(),
    @Transient
    var lastAccess: Long = -1,
) {
    fun mute(): Long {
        val now = System.currentTimeMillis()
        if (now - muteUntil <= config.muteHandler.muteDurationMultiplier.maxMuteInterval) {
            muteMultiplier *= config.muteHandler.muteDurationMultiplier.multiplier
        } else {
            muteMultiplier = 1.0
        }

        val currMute = if (muteUntil > now) muteUntil - now else 0
        muteUntil = (now + config.muteHandler.baseMuteDuration * muteMultiplier).toLong()
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

    fun purge(): SpamData {
        val now = System.currentTimeMillis()
        records.removeHeadIf { config.expireTime.messageRecord.expired(now - it.time) }
        if (config.expireTime.whisperTarget >= 0)
            whisperTargets.removeIf { now - it.lastTime > config.expireTime.whisperTarget }
        if (config.expireTime.chatRequest >= 0)
            requests.removeHeadIf { now - it > config.expireTime.chatRequest }
        if (config.expireTime.filtered >= 0)
            filtered.removeHeadIf { now - it > config.expireTime.filtered }
        if (config.expireTime.score >= 0)
            scores.removeHeadIf { now - it.time > config.expireTime.score }
        return this
    }

    private fun <T> ArrayDeque<T>.removeHeadIf(predicate: (T) -> Boolean) {
        val firstNotMatch = this.indexOfFirst { !predicate.invoke(it) }
        for (i in 0 ..< firstNotMatch) {
            this.removeFirst()
        }
    }

    data class MessageRecord(
        @SerializedName("m", alternate = ["message"])
        val message: String,
        @SerializedName("t", alternate = ["time"])
        var time: Long,
    )

    data class SpamScore(
        @SerializedName("k", alternate = ["type"])
        val type: String,
        @SerializedName("s", alternate = ["score"])
        var score: Double,
        @SerializedName("t", alternate = ["time"])
        var time: Long
    )

    data class WhisperTarget(
        @SerializedName("p", alternate = ["player"])
        val player: String,
        @SerializedName("t", alternate = ["lastTime"])
        var lastTime: Long,
    )
}