package io.github.rothes.esu.bukkit.module.chatantispam.message.meta

class WhisperMessage(
    val receiver: String,
): BaseMessageMeta(MessageType.WHISPER) {

    override fun toString(): String {
        return "$typeName $receiver"
    }
}