package io.github.rothes.esu.bukkit.module.chatantispam.check

import info.debatty.java.stringsimilarity.RatcliffObershelp
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import kotlin.math.max
import kotlin.math.pow

object Similarity: Check() {

    private val ro = RatcliffObershelp() // https://github.com/tdebatty/java-string-similarity

    override fun check(request: MessageRequest): CheckResult {
        with(request.spamCheck.similarityCheck) {
            val allowCount = blockOnDisallowCount.entries.firstOrNull { it.key >= request.rawMessage.length }?.value
                ?: blockOnDisallowCount.lastEntry().value
            if (allowCount > 0) {
                val spamData = request.spamData
                val message = request.message
                val time = request.sendTime
                val hit = ArrayList<Double>(allowCount)
                val allowedSim = max(lowestAllowRate, baseAllowRate - allowRateReducePerRecord * spamData.records.size)
                spamData.records.forEach { record ->
                    val similarity = ro.similarity(record.message, message) *
                            config.expireTime.messageRecord.rate(time - record.time)
                    if (similarity >= allowedSim) {
                        hit.add(similarity)
                        if (hit.size == allowCount) {
                            val avg = hit.average()
                            return CheckResult("sim " + String.format("%.2f", avg), avg.pow(2), false,
                                addFilter = request.messageMeta.type != MessageType.DEATH)
                        }
                    }
                }
            }
        }
        return CheckResult()
    }
}