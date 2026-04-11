package io.github.rothes.esu.bukkit.module.chatantispam.message.meta

abstract class BaseMessageMeta(
    override val type: MessageType
): MessageMeta {

    val typeName: String
        get() = javaClass.simpleName.removeSuffix("Message").lowercase()

    override fun toString(): String {
        return typeName
    }

}