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
