/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
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
        val esuNativeBook = versioned<EsuNativeBook<*>>()
        if (esuNativeBook.isSupported) {
            facetBook.add(0, esuNativeBook)
        }
    }

    private object AnsiFlattener {

        private val PATTERN = "%(?:(\\d+)\\$)?s".toPattern()

        fun init() {
            val flattener = ComponentFlattener.basic().toBuilder()
                .complexMapper(TranslatableComponent::class.java) { translatable, consumer ->
                    // Refer to io.papermc.paper.adventure.PaperAdventure.FLATTENER
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