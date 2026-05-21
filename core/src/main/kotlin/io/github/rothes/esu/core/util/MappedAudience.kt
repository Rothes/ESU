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

import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.audience.MessageType
import io.github.rothes.esu.lib.adventure.bossbar.BossBar
import io.github.rothes.esu.lib.adventure.chat.SignedMessage
import io.github.rothes.esu.lib.adventure.dialog.DialogLike
import io.github.rothes.esu.lib.adventure.identity.Identity
import io.github.rothes.esu.lib.adventure.inventory.Book
import io.github.rothes.esu.lib.adventure.resource.ResourcePackRequest
import io.github.rothes.esu.lib.adventure.sound.Sound
import io.github.rothes.esu.lib.adventure.sound.SoundStop
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.title.Title
import io.github.rothes.esu.lib.adventure.title.TitlePart
import java.util.*
import net.kyori.adventure.audience.Audience as ServerAudience
import net.kyori.adventure.text.Component as ServerComponent
import net.kyori.adventure.title.Title as ServerTitle

class MappedAudience(
    private val audience: ServerAudience
): Audience {

    override fun sendMessage(message: Component) {
        audience.sendMessage(message.server)
    }

    override fun sendMessage(source: Identity, message: Component, type: MessageType) {
        TODO()
    }

    override fun deleteMessage(signature: SignedMessage.Signature) {
        TODO()
    }

    override fun sendActionBar(message: Component) {
        audience.sendActionBar(message.server)
    }

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
        audience.sendPlayerListHeaderAndFooter(header.server, footer.server)
    }

    override fun showTitle(title: Title) {
        audience.showTitle(title.server)
    }

    override fun <T : Any?> sendTitlePart(part: TitlePart<T>, value: T & Any) {
        when (part) {
            TitlePart.TITLE, TitlePart.SUBTITLE -> {
                @Suppress("UNCHECKED_CAST")
                val server = part.server as net.kyori.adventure.title.TitlePart<ServerComponent>
                audience.sendTitlePart(server, (value as Component).server)
            }
            TitlePart.TIMES -> {
                @Suppress("UNCHECKED_CAST")
                val server = part.server as net.kyori.adventure.title.TitlePart<ServerTitle.Times>
                audience.sendTitlePart(server, (value as Title.Times).server)
            }
            else -> error("Unknown title part $this")
        }
    }

    override fun clearTitle() {
        audience.clearTitle()
    }

    override fun resetTitle() {
        audience.resetTitle()
    }

    override fun showBossBar(bar: BossBar) {
        audience.showBossBar(bar.server)
    }

    override fun hideBossBar(bar: BossBar) {
        audience.hideBossBar(bar.server)
    }

    override fun playSound(sound: Sound) {
        audience.playSound(sound.server)
    }

    override fun playSound(sound: Sound, x: Double, y: Double, z: Double) {
        audience.playSound(sound.server, x, y, z)
    }

    override fun playSound(sound: Sound, emitter: Sound.Emitter) {
        audience.playSound(sound.server, emitter.server)
    }

    override fun stopSound(sound: Sound) {
        audience.stopSound(sound.server)
    }

    override fun stopSound(stop: SoundStop) {
        TODO()
    }

    override fun openBook(book: Book) {
        audience.openBook(book.server)
    }

    override fun sendResourcePacks(request: ResourcePackRequest) {
        TODO()
    }

    override fun removeResourcePacks(id: UUID, vararg others: UUID) {
        TODO()
    }

    override fun clearResourcePacks() {
        TODO()
    }

    override fun showDialog(dialog: DialogLike) {
        TODO()
    }

    override fun closeDialog() {
        TODO()
    }

}