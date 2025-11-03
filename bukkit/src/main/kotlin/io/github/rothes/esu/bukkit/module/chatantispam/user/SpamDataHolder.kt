package io.github.rothes.esu.bukkit.module.chatantispam.user

data class SpamDataHolder(
    @get:Synchronized
    @set:Synchronized
    var spamData: SpamData
)
