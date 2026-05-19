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

package io.github.rothes.esu.core.configuration.data

import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.adventure.title.Title
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlin.time.Duration as KtDuration

data class TitleData(
    val title: String? = null,
    val subTitle: String? = null,
    val times: Times? = null,
) {

    fun parsed(user: User, vararg params: TagResolver): ParsedMessageData.ParsedTitleData {
        return ParsedMessageData.ParsedTitleData(
            title?.let { user.buildMiniMessage(it, *params) },
            subTitle?.let { user.buildMiniMessage(it, *params) },
            times
        )
    }

    data class Times(
        val fadeIn: Duration = Duration.ofMillis(500),
        val stay: Duration = Duration.ofMillis(3500),
        val fadeOut: Duration = Duration.ofMillis(1000),
    ) {
        constructor(fadeIn: KtDuration, stay: KtDuration, fadeOut: KtDuration) : this(fadeIn.toJavaDuration(), stay.toJavaDuration(), fadeOut.toJavaDuration())

        val adventure by lazy { Title.Times.times(fadeIn, stay, fadeOut) }

        companion object {
            fun ofTicks(fadeIn: Long, stay: Long, fadeOut: Long) = Times((fadeIn * 50).milliseconds, (stay * 50).milliseconds, (fadeOut * 50).milliseconds)
        }
    }
}
