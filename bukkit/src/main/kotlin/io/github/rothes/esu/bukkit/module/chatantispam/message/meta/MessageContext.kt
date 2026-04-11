package io.github.rothes.esu.bukkit.module.chatantispam.message.meta

interface MessageContext {

    val type: MessageType

    val createdByOwn: Boolean

    override fun toString(): String

}