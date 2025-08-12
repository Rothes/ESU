package io.github.rothes.esu.core.util

import io.github.rothes.esu.lib.net.kyori.adventure.bossbar.BossBar
import io.github.rothes.esu.lib.net.kyori.adventure.inventory.Book
import io.github.rothes.esu.lib.net.kyori.adventure.key.Key
import io.github.rothes.esu.lib.net.kyori.adventure.sound.Sound
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import io.github.rothes.esu.lib.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import io.github.rothes.esu.lib.net.kyori.adventure.title.Title
import io.github.rothes.esu.lib.net.kyori.adventure.title.TitlePart
import java.util.WeakHashMap

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
        get() = net.kyori.adventure.title.Title.Times.of(fadeIn(), stay(), fadeOut())

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