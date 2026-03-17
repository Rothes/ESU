package io.github.rothes.esu.bukkit.module.core.persistence

import com.google.gson.annotations.SerializedName

data class PersistentData(
    @SerializedName("at")
    val attackTime: Long,
    @SerializedName("gat")
    val genericActiveTime: Long,
    @SerializedName("mt")
    val moveTime: Long,
    @SerializedName("pmt")
    val posMoveTime: Long,
)
