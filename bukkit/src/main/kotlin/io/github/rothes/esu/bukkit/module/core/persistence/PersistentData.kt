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

package io.github.rothes.esu.bukkit.module.core.persistence

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class PersistentData(
    @SerializedName("lad")
    val lastActionDuration: LastActionDuration,
) {

    @SerializedName("at")
    @field:Expose(serialize = false)
    val attackTime: Long = 0
    @SerializedName("gat")
    @field:Expose(serialize = false)
    val genericActiveTime: Long = 0
    @SerializedName("mt")
    @field:Expose(serialize = false)
    val moveTime: Long = 0
    @SerializedName("pmt")
    @field:Expose(serialize = false)
    val posMoveTime: Long = 0

    data class LastActionDuration(
        @SerializedName("a")
        val attack: Long,
        @SerializedName("g")
        val generic: Long,
        @SerializedName("m")
        val move: Long,
        @SerializedName("p")
        val posMove: Long,
    )
}
