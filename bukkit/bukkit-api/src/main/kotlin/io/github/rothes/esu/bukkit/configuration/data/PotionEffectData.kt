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

package io.github.rothes.esu.bukkit.configuration.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeIf
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import java.time.Duration as JDuration

data class PotionEffectData(
    val type: String = "speed",
    val duration: JDuration = 1.seconds.toJavaDuration(),
    @NoDeserializeIf("1")
    val level: Int = 1,
    @NoDeserializeIf("true")
    val ambient: Boolean = true,
    @NoDeserializeIf("true")
    val particles: Boolean = true,
    @NoDeserializeIf("true")
    val icon: Boolean = true,
): ConfigurationPart {

    val bukkit
        get() = PotionEffect(
            PotionEffectType.getByKey(NamespacedKey.fromString(type)) ?: error("Unknown potion type: $type"),
            (duration.toMillis() / 50).toInt(),
            level - 1,
            ambient, particles, icon
        )
}
