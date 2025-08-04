package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer
import net.kyori.ansi.ColorLevel

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
            message(message.copy(chat = message.chat.map { "[ESU] $it" }), params = params)
    }

    override fun message(message: Component) {
        if (EsuConfig.get().forceTrueColorConsole)
            print(serializer.serialize(message))
        else
            super.message(message)
    }

    fun print(string: String)

    companion object {
        private val serializer = ANSIComponentSerializer.builder().colorLevel(ColorLevel.TRUE_COLOR).build()
    }

}