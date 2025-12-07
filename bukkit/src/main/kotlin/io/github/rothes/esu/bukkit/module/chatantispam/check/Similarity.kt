package io.github.rothes.esu.bukkit.module.chatantispam.check

import info.debatty.java.stringsimilarity.RatcliffObershelp
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.util.extension.charSize
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import kotlin.math.max
import kotlin.math.pow

object Similarity: Check("similarity") {

    override val defaultBlockedMessage = "<ec>You are sending messages that are too similar.".message

    private val ro = RatcliffObershelp() // https://github.com/tdebatty/java-string-similarity

    override fun check(request: MessageRequest): CheckResult {
        with(request.spamCheck.similarityCheck) {
            val allowCount = blockOnDisallowCount.entries.firstOrNull { it.key >= request.rawMessage.charSize() }?.value
                ?: blockOnDisallowCount.lastEntry().value
            if (allowCount > 0) {
                val spamData = request.spamData
                val message = request.message
                val time = request.sendTime
                val hit = DoubleArrayList(allowCount)
                val afkMp = afkRateMultiplier.entries.firstOrNull { it.key.inWholeMilliseconds > request.afkTime }?.value ?: 1.0
                val allowedSim = max(lowestAllowRate, baseAllowRate - allowRateReducePerRecord * spamData.records.size)
                spamData.records.forEach { record ->
                    val similarity = (
                        ro.similarity(record.message, message) *
                        config.expireTime.messageRecord.rate(time - record.time, request.afkTime) *
                        afkMp
                    ).coerceAtMost(1.0)
                    if (similarity >= allowedSim) {
                        hit.add(similarity)
                        if (hit.size == allowCount) {
                            val avg = hit.average()
                            val notify = request.messageMeta.type != MessageType.DEATH
                            if (notify)
                                notifyBlocked(request.user)
                            return CheckResult(
                                buildString {
                                    append("sim ")
                                    append("%.2f".format(avg))
                                    if (afkMp != 1.0) {
                                        append(" x")
                                        append(afkMp)
                                    }
                                },
                                avg.pow(2),
                                false,
                                addFilter = notify
                            )
                        }
                    }
                }
            }
        }
        return CheckResult()
    }
}