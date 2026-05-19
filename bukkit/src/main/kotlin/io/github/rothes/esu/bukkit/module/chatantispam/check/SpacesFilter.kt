/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest

object SpacesFilter: Check("spaces-filter") {

    private val duplicateSpaceRegex = "[\\s\\u00A0\\u1680\\u180E\\u2000-\\u200B\\u202F\\u205F\\u3000\\uFEFF]+".toRegex()

    override fun check(request: MessageRequest): CheckResult {
        if (!request.context.createdByOwn) {
            return CheckResult()
        }
        val spacesCheck = request.spamCheck.spaces
        val message = request.message
        if (spacesCheck.minLength >= 0 && spacesCheck.minLength <= message.length) {
            val spaceCount = message.filter { it.isWhitespace() }.length
            val spaceRate = spaceCount / message.length.toDouble()
            if (spaceRate >= spacesCheck.spaceRate) {
                return CheckResult("spaces", 0.275)
            }
        }

        if (spacesCheck.removeExtraSpacesOnCheck) {
            request.message = request.message.replace(duplicateSpaceRegex, " ")
        }
        return CheckResult()
    }
}