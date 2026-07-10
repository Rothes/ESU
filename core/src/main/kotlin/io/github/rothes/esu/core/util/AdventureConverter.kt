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

package io.github.rothes.esu.core.util

import io.github.rothes.esu.lib.adventure.bossbar.BossBar
import io.github.rothes.esu.lib.adventure.inventory.Book
import io.github.rothes.esu.lib.adventure.key.Key
import io.github.rothes.esu.lib.adventure.sound.Sound
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.serializer.gson.GsonComponentSerializer
import io.github.rothes.esu.lib.adventure.title.Title
import io.github.rothes.esu.lib.adventure.title.TitlePart
import java.util.*

object AdventureConverter {

    private val bossBarMapper = WeakHashMap<BossBar, net.kyori.adventure.bossbar.BossBar>()

    val Key.server
        get() = net.kyori.adventure.key.Key.key(namespace(), value())

    val net.kyori.adventure.text.Component.esu
        get() = GsonComponentSerializer.gson().deserialize(net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(this))

    val Component.server
        get() = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(GsonComponentSerializer.gson().serialize(this))

    val Book.server
        get() = net.kyori.adventure.inventory.Book.book(title().server, author().server, pages().map { it.server })

    val BossBar.server
        get() = bossBarMapper.getOrPut(this) {
            net.kyori.adventure.bossbar.BossBar.bossBar(name().server, progress(), color().server, overlay().server, flags().map { it.server }.toSet())
        }

    val BossBar.Color.server
        get() = net.kyori.adventure.bossbar.BossBar.Color.valueOf(name)

    val BossBar.Flag.server
        get() = net.kyori.adventure.bossbar.BossBar.Flag.valueOf(name)

    val BossBar.Overlay.server
        get() = net.kyori.adventure.bossbar.BossBar.Overlay.valueOf(name)

    val TitlePart<*>.server
        get() = when (this) {
            TitlePart.TITLE -> net.kyori.adventure.title.TitlePart.TITLE
            TitlePart.SUBTITLE -> net.kyori.adventure.title.TitlePart.SUBTITLE
            TitlePart.TIMES -> net.kyori.adventure.title.TitlePart.TIMES
            else -> error("Unknown title part $this")
        }

    val Title.Times.server
        get() = net.kyori.adventure.title.Title.Times.times(fadeIn(), stay(), fadeOut())

    val Title.server
        get() = net.kyori.adventure.title.Title.title(title().server, subtitle().server, times()?.server)

    val Sound.server
        get() = net.kyori.adventure.sound.Sound.sound()
            .type(name().server)
            .source(source().server)
            .pitch(pitch())
            .volume(volume())
            .seed(seed())
            .build()

    val Sound.Source.server
        get() = net.kyori.adventure.sound.Sound.Source.valueOf(name)

    val Sound.Emitter.server
        get() = when (this) {
            Sound.Emitter.self() -> net.kyori.adventure.sound.Sound.Emitter.self()
            is ServerEmitter -> emitter
            else -> error("Unknown emitter $this")
        }

    class ServerEmitter(val emitter: net.kyori.adventure.sound.Sound.Emitter): Sound.Emitter

}