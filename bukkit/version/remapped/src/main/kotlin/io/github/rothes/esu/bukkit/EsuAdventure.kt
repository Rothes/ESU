package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.EsuNativeBook
import io.github.rothes.esu.core.util.ComponentUtils
import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGetT
import io.github.rothes.esu.lib.adventure.platform.facet.Facet
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.TranslatableComponent
import io.github.rothes.esu.lib.adventure.text.flattener.ComponentFlattener
import io.github.rothes.esu.lib.adventure.translation.GlobalTranslator
import net.minecraft.locale.Language
import org.bukkit.entity.Player
import java.util.*

@Suppress("UnstableApiUsage")
object EsuAdventure {

    fun inject() {
        initAudience()
        AnsiFlattener.init()
    }

    fun initAudience() {
        val adventureAudience = Class.forName("io.github.rothes.esu.lib.adventure.platform.bukkit.BukkitAudience")
        val facetBook = adventureAudience.getDeclaredField("BOOK")
            .accessibleGetT<MutableList<Facet.Book<Player, *, *>>>(null)
        val esuNativeBook by Versioned(EsuNativeBook::class.java)
        if (esuNativeBook.isSupported) {
            facetBook.add(0, esuNativeBook)
        }
    }

    private object AnsiFlattener {

        private val PATTERN = "%(?:(\\d+)\\$)?s".toPattern()

        fun init() {
            val flattener = ComponentFlattener.basic().toBuilder()
                .complexMapper(TranslatableComponent::class.java) { translatable, consumer ->
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
                    val args = translatable.arguments()
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
                }.build()
            ComponentUtils.flattener = flattener
        }
    }

}