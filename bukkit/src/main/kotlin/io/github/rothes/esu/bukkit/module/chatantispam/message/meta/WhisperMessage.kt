package io.github.rothes.esu.bukkit.module.chatantispam.message.meta

class WhisperMessage(
    val receiver: String,
): BaseMessageContext(MessageType.WHISPER) {

    override fun toString(): String {
        return "$typeName $receiver"
    }
}