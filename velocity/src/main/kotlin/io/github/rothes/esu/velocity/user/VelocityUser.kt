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

package io.github.rothes.esu.velocity.user

import com.velocitypowered.api.command.CommandSource
import io.github.rothes.esu.common.user.CommonUser
import io.github.rothes.esu.core.util.MappedAudience
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.bossbar.BossBar
import io.github.rothes.esu.lib.adventure.chat.SignedMessage
import io.github.rothes.esu.lib.adventure.dialog.DialogLike
import io.github.rothes.esu.lib.adventure.inventory.Book
import io.github.rothes.esu.lib.adventure.resource.ResourcePackRequest
import io.github.rothes.esu.lib.adventure.sound.Sound
import io.github.rothes.esu.lib.adventure.sound.SoundStop
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.title.Title
import io.github.rothes.esu.lib.adventure.title.TitlePart
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate

abstract class VelocityUser: CommonUser() {

    abstract override val commandSender: CommandSource

    override fun hasPermission(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }

    /************************
     #  Adventure Audience  #
     ************************/
    override fun message(message: net.kyori.adventure.text.Component) {
        commandSender.sendMessage(message)
    }
    override fun actionBar(message: net.kyori.adventure.text.Component) {
        commandSender.sendActionBar(message)
    }

    // Audience impl start
    private var _audience: MappedAudience? = null

    val mappedAudience: Audience
        get() = _audience ?: MappedAudience(commandSender).also { _audience = it }

    override fun playSound(sound: Sound) {
        mappedAudience.playSound(sound)
    }

    override fun playSound(
        sound: Sound, x: Double, y: Double, z: Double
    ) {
        mappedAudience.playSound(sound, x, y, z)
    }

    override fun playSound(
        sound: Sound, emitter: Sound.Emitter
    ) {
        mappedAudience.playSound(sound, emitter)
    }

    override fun filterAudience(filter: Predicate<in Audience>): Audience {
        return mappedAudience.filterAudience(filter)
    }

    override fun forEachAudience(action: Consumer<in Audience>) {
        mappedAudience.forEachAudience(action)
    }

    override fun sendMessage(message: Component) {
        mappedAudience.sendMessage(message) // Impl for adventure4
    }

    override fun deleteMessage(signature: SignedMessage.Signature) {
        mappedAudience.deleteMessage(signature)
    }

    override fun sendActionBar(message: Component) {
        mappedAudience.sendActionBar(message)
    }

    override fun sendPlayerListHeader(header: Component) {
        mappedAudience.sendPlayerListHeader(header)
    }

    override fun sendPlayerListFooter(footer: Component) {
        mappedAudience.sendPlayerListFooter(footer)
    }

    override fun sendPlayerListHeaderAndFooter(
        header: Component, footer: Component
    ) {
        mappedAudience.sendPlayerListHeaderAndFooter(header, footer)
    }

    override fun showTitle(title: Title) {
        mappedAudience.showTitle(title)
    }

    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T) {
        mappedAudience.sendTitlePart(part, value)
    }

    override fun clearTitle() {
        mappedAudience.clearTitle()
    }

    override fun resetTitle() {
        mappedAudience.resetTitle()
    }

    override fun showBossBar(bar: BossBar) {
        mappedAudience.showBossBar(bar)
    }

    override fun hideBossBar(bar: BossBar) {
        mappedAudience.hideBossBar(bar)
    }

    override fun stopSound(sound: Sound) {
        mappedAudience.stopSound(sound)
    }

    override fun stopSound(stop: SoundStop) {
        mappedAudience.stopSound(stop)
    }

    override fun openBook(book: Book.Builder) {
        mappedAudience.openBook(book)
    }

    override fun openBook(book: Book) {
        mappedAudience.openBook(book)
    }

    override fun sendResourcePacks(request: ResourcePackRequest) {
        mappedAudience.sendResourcePacks(request)
    }

    override fun removeResourcePacks(request: ResourcePackRequest) {
        mappedAudience.removeResourcePacks(request)
    }

    override fun removeResourcePacks(id: UUID, vararg others: UUID) {
        mappedAudience.removeResourcePacks(id, *others)
    }

    override fun clearResourcePacks() {
        mappedAudience.clearResourcePacks()
    }

    override fun showDialog(dialog: DialogLike) {
        mappedAudience.showDialog(dialog)
    }

    override fun closeDialog() {
        mappedAudience.closeDialog()
    }
}