package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

interface LogUser: User {

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("logMessage")
    fun <T: ConfigurationPart> log(locales: MultiLocaleConfiguration<T>, block: T.() -> MessageData?, vararg params: TagResolver) {
        val messageData = localed(locales, block)
        log(messageData, params = params)
    }

    fun <T: ConfigurationPart> log(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        val message = localed(locales, block)
        minimessage("[ESU] $message", params = params)
    }

    fun log(message: String, vararg params: TagResolver) {
        if (message.isEmpty())
            minimessage("[ESU] $message", params = params)
    }

    fun log(message: MessageData, vararg params: TagResolver) {
        if (message.chat.isNullOrEmpty())
            message(message, params = params)
        else
            message(message.copy(chat = "[ESU] " + message.chat), params = params)
    }

}