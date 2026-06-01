/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.chatantispam.check

import info.debatty.java.stringsimilarity.RatcliffObershelp
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.util.extension.charSize
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.pow

object Similarity: Check("similarity") {

    override val defaultBlockedMessage = "<ec>You are sending messages that are too similar.".message

    private val ro = RatcliffObershelp() // https://github.com/tdebatty/java-string-similarity

    override fun check(request: MessageRequest): CheckResult {
        with(request.spamCheck.similarityCheck) {
            val recordConfig = config.expireTime.messageRecord
            val charSize = request.rawMessage.charSize()
            val allowCount = blockOnDisallowCount.entries.firstOrNull { it.key >= charSize }?.value
                ?: blockOnDisallowCount.lastEntry().value
            val charMp = charSizeMultiplier.entries.firstOrNull { charSize >= it.key }?.value
                ?: 1.0
            if (allowCount > 0) {
                val spamData = request.spamData
                val message = request.message
                val time = request.sendTime
                val hit = DoubleArrayList(allowCount)
                val afkMp = afkRateMultiplier.entries.firstOrNull { request.afkTime >= it.key.inWholeMilliseconds }?.value ?: 1.0
                val allowedSim = max(lowestAllowRate, baseAllowRate - allowRateReducePerRecord * spamData.nonExpiredRecords(time, request.afkTime))
                spamData.records.forEach { record ->
                    val expireMp = recordConfig.rate(time - record.time, request.afkTime)
                        .coerceAtLeast(recordConfig.minExpireRateMultiplier)

                    val similarityRaw = ro.similarity(record.message, message)
                    val similarity = similarityRaw * expireMp * afkMp * charMp
                    if (similarity >= allowedSim) {
                        hit.add(similarity)
                        if (hit.size == allowCount) {
                            val avg = hit.average()
                            val notify = request.context.createdByOwn
                            if (notify)
                                notifyBlocked(request.user)
                            return CheckResult(
                                buildString {
                                    append("sim ")
                                    append("%.2f".format(avg))
                                    if (expireMp != 1.0) {
                                        append(" ex")
                                        append(DecimalFormat("###.##").format(expireMp))
                                    }
                                    if (afkMp != 1.0) {
                                        append(" ax")
                                        append(afkMp)
                                    }
                                    if (charMp != 1.0) {
                                        append(" cx")
                                        append(charMp)
                                    }
                                },
                                avg.coerceAtMost(1.0).pow(2),
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