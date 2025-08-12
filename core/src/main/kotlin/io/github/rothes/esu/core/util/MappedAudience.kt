package io.github.rothes.esu.core.util

import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.net.kyori.adventure.audience.Audience
import io.github.rothes.esu.lib.net.kyori.adventure.audience.MessageType
import io.github.rothes.esu.lib.net.kyori.adventure.bossbar.BossBar
import io.github.rothes.esu.lib.net.kyori.adventure.chat.SignedMessage
import io.github.rothes.esu.lib.net.kyori.adventure.dialog.DialogLike
import io.github.rothes.esu.lib.net.kyori.adventure.identity.Identity
import io.github.rothes.esu.lib.net.kyori.adventure.inventory.Book
import io.github.rothes.esu.lib.net.kyori.adventure.resource.ResourcePackRequest
import io.github.rothes.esu.lib.net.kyori.adventure.sound.Sound
import io.github.rothes.esu.lib.net.kyori.adventure.sound.SoundStop
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import io.github.rothes.esu.lib.net.kyori.adventure.title.Title
import io.github.rothes.esu.lib.net.kyori.adventure.title.TitlePart
import java.util.UUID
import net.kyori.adventure.title.Title as ServerTitle
import net.kyori.adventure.audience.Audience as ServerAudience
import net.kyori.adventure.text.Component as ServerComponent

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