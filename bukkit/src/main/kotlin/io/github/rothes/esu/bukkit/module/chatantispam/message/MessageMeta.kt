package io.github.rothes.esu.bukkit.module.chatantispam.message

open class MessageMeta(val type: MessageType, val receiver: String?) {

    override fun toString(): String {
        return buildString {
            append(type.name.lowercase())
            receiver?.let { append(' ').append(it) }
        }
    }
}