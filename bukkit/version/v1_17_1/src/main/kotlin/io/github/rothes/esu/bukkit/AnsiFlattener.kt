package io.github.rothes.esu.bukkit

import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import io.github.rothes.esu.lib.net.kyori.adventure.text.TranslatableComponent
import io.github.rothes.esu.lib.net.kyori.adventure.text.flattener.ComponentFlattener
import io.github.rothes.esu.lib.net.kyori.adventure.translation.GlobalTranslator
import net.minecraft.locale.Language
import java.util.Locale

object AnsiFlattener {

    private val PATTERN = "%(?:(\\d+)\\$)?s".toPattern()

    init {
        LogUser.setFlattener(ComponentFlattener.basic().toBuilder().complexMapper(TranslatableComponent::class.java) { translatable, consumer ->
            val language = Language.getInstance()
            val fallback = translatable.fallback()
            if (!language.has(translatable.key()) && (fallback == null || !language.has(fallback))) {
                if (GlobalTranslator.translator().canTranslate(translatable.key(), Locale.US)) {
                    consumer.accept(GlobalTranslator.render(translatable, Locale.US))
                    return@complexMapper
                }
            }

            if (!language.has(translatable.key())) {
                consumer.accept(Component.text(fallback ?: translatable.key()))
                return@complexMapper
            }

            val translated = language.getOrDefault(translatable.key())
            val matcher = PATTERN.matcher(translated)
            val args = translatable.args() // arguments() is not there on Paper 1.20.1
            var argId = 0
            var right = 0
            while (matcher.find()) {
                if (right < matcher.start()) {
                    consumer.accept(Component.text(translated.substring(right, matcher.start())))
                }
                right = matcher.end()

                val placeholder = matcher.group(1)
                if (placeholder != null) {
                    placeholder.toIntOrNull()?.let {
                        val index = it - 1
                        if (index < args.size) {
                            consumer.accept(args[index].asComponent())
                        }
                    }
                } else {
                    val index = argId++
                    consumer.accept(args[index].asComponent())
                }
            }

            if (right < translated.length) {
                consumer.accept(Component.text(translated.substring(right)))
            }
        }.build())
    }
}