package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.esu.core.util.extension.ifLet
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.TranslatableComponent
import io.github.rothes.esu.lib.adventure.text.flattener.ComponentFlattener
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.adventure.text.serializer.ansi.ANSIComponentSerializer
import io.github.rothes.esu.lib.adventure.translation.GlobalTranslator
import io.github.rothes.esu.lib.adventure.translation.TranslationRegistry
import io.github.rothes.esu.lib.net.kyori.ansi.ColorLevel
import org.slf4j.Logger
import java.util.*

interface LogUser: User {

    val logger: Logger

    fun print(message: String)

    override fun actionBar(message: Component) {
        message(message) // Relocate actionBar messages to console message
    }

    override fun message(message: Component) {
        print(serializer.serialize(message))
    }

    /*  Slf4J info  */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("infoMessage")
    fun <T: ConfigurationPart> info(locales: MultiLangConfiguration<T>, block: T.() -> MessageData?, vararg params: TagResolver, prefix: String = "") {
        val messageData = localed(locales, block)
        info(messageData, params = params)
    }

    fun <T: ConfigurationPart> info(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver, prefix: String = "") {
        val message = localed(locales, block)
        infoMiniMessage(message, params = params)
    }

    fun info(message: String, vararg params: TagResolver, prefix: String = "") {
        infoMiniMessage(message, params = params)
    }

    fun info(message: MessageData, vararg params: TagResolver, prefix: String = "") {
        info(message.parsed(this, params = params), prefix)
    }

    fun info(messageData: ParsedMessageData, prefix: String = "") {
        with(messageData) {
            chat?.let { it.forEach { msg -> info(msg, prefix) } }
            actionBar?.let { actionBar(it) }
            title?.let { title(it) }
            sound?.let { playSound(it) }
        }
    }

    fun info(message: Component, prefix: String = "") {
        logger.info(prefix.ifLet(prefix.isNotEmpty()) { "[$this] " } + serializer.serialize(message))
    }

    fun infoMiniMessage(message: String, vararg params: TagResolver, prefix: String = "") {
        info(buildMiniMessage(message, params = params), prefix)
    }

    /*  Slf4J warn  */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("warnMessage")
    fun <T: ConfigurationPart> warn(locales: MultiLangConfiguration<T>, block: T.() -> MessageData?, vararg params: TagResolver, prefix: String = "") {
        val messageData = localed(locales, block)
        warn(messageData, params = params)
    }

    fun <T: ConfigurationPart> warn(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver, prefix: String = "") {
        val message = localed(locales, block)
        warnMiniMessage(message, params = params)
    }

    fun warn(message: String, vararg params: TagResolver, prefix: String = "") {
        warnMiniMessage(message, params = params)
    }

    fun warn(message: MessageData, vararg params: TagResolver, prefix: String = "") {
        warn(message.parsed(this, params = params), prefix)
    }

    fun warn(messageData: ParsedMessageData, prefix: String = "") {
        with(messageData) {
            chat?.let { it.forEach { msg -> warn(msg, prefix) } }
            actionBar?.let { actionBar(it) }
            title?.let { title(it) }
            sound?.let { playSound(it) }
        }
    }

    fun warn(message: Component, prefix: String = "") {
        logger.warn(prefix.ifLet(prefix.isNotEmpty()) { "[$this] " } + serializer.serialize(message))
    }

    fun warnMiniMessage(message: String, vararg params: TagResolver, prefix: String = "") {
        warn(buildMiniMessage(message, params = params), prefix)
    }

    /*  Slf4J error  */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("errorMessage")
    fun <T: ConfigurationPart> error(locales: MultiLangConfiguration<T>, block: T.() -> MessageData?, vararg params: TagResolver, prefix: String = "") {
        val messageData = localed(locales, block)
        error(messageData, params = params)
    }

    fun <T: ConfigurationPart> error(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver, prefix: String = "") {
        val message = localed(locales, block)
        errorMiniMessage(message, params = params)
    }

    fun error(message: String, vararg params: TagResolver, prefix: String = "") {
        errorMiniMessage(message, params = params)
    }

    fun error(message: MessageData, vararg params: TagResolver, prefix: String = "") {
        error(message.parsed(this, params = params), prefix)
    }

    fun error(messageData: ParsedMessageData, prefix: String = "") {
        with(messageData) {
            chat?.let { it.forEach { msg -> error(msg, prefix) } }
            actionBar?.let { actionBar(it) }
            title?.let { title(it) }
            sound?.let { playSound(it) }
        }
    }

    fun error(message: Component, prefix: String = "") {
        logger.error(prefix.ifLet(prefix.isNotEmpty()) { "[$this] " } + serializer.serialize(message))
    }

    fun errorMiniMessage(message: String, vararg params: TagResolver, prefix: String = "") {
        error(buildMiniMessage(message, params = params), prefix)
    }


    companion object {

        var console: LogUser by InitOnce()

        private var flattener = ComponentFlattener.basic().toBuilder().complexMapper(TranslatableComponent::class.java) { translatable, consumer ->
            for (source in GlobalTranslator.translator().sources()) {
                if (source is TranslationRegistry && source.contains(translatable.key())) {
                    consumer.accept(GlobalTranslator.render(translatable, Locale.getDefault()))
                    return@complexMapper
                }
            }
            val fallback = translatable.fallback() ?: return@complexMapper
            for (source in GlobalTranslator.translator().sources()) {
                if (source is TranslationRegistry && source.contains(fallback)) {
                    consumer.accept(GlobalTranslator.render(Component.translatable(fallback), Locale.getDefault()))
                    return@complexMapper
                }
            }
        }.build()

        private var serializer = buildSerializer()

        fun setFlattener(flattener: ComponentFlattener) {
            this.flattener = flattener
            this.serializer = buildSerializer() // flattener updated
        }

        fun onReload() {
            this.serializer = buildSerializer()
        }

        private fun buildSerializer() = ANSIComponentSerializer.builder()
            .colorLevel(if (EsuConfig.get().forceTrueColorConsole) ColorLevel.TRUE_COLOR else ColorLevel.compute())
            .flattener(flattener)
            .build()
    }

}