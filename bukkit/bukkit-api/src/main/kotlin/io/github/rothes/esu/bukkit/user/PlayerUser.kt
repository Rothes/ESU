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

package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.connected
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.BossBarImplementationImpl
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.EsuBukkitEmitter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ComponentSerializer
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.lib.adventure.bossbar.BossBar
import io.github.rothes.esu.lib.adventure.bossbar.BossBarImplementation
import io.github.rothes.esu.lib.adventure.chat.ChatType
import io.github.rothes.esu.lib.adventure.chat.SignedMessage
import io.github.rothes.esu.lib.adventure.dialog.DialogLike
import io.github.rothes.esu.lib.adventure.inventory.Book
import io.github.rothes.esu.lib.adventure.resource.ResourcePackCallback
import io.github.rothes.esu.lib.adventure.resource.ResourcePackRequest
import io.github.rothes.esu.lib.adventure.resource.ResourcePackStatus
import io.github.rothes.esu.lib.adventure.sound.Sound
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.flattener.ComponentFlattener
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.adventure.title.Title
import io.github.rothes.esu.lib.adventure.title.TitlePart
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.MessageSignature
import net.minecraft.network.chat.OutgoingChatMessage
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.game.*
import net.minecraft.resources.Identifier
import net.minecraft.server.network.Filterable
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WrittenBookContent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerUser(override val uuid: UUID, initPlayer: Player? = null): BukkitUser() {

    constructor(player: Player): this(player.uniqueId, player)

    var playerCache: Player? = initPlayer
        get() {
            val cache = field
            if (cache != null) {
                // Check if the instance is as it is.
                if (cache.connected) {
                    return cache
                }
            }
            val get = Bukkit.getPlayer(uuid)
            if (get != null) {
                field = get
                return get
            }
            return cache
        }
        internal set
    val player: Player
        get() = playerCache ?: error("Player $uuid is not online and there's no cached instance!")
    override val commandSender: CommandSender
        get() = player
    override val dbId: Int
    override val dbName: String?
    override val nameUnsafe: String?
        get() = playerCache?.name
    override val clientLocale: String
        get() = player.locale

    override var languageUnsafe: String?
    override var colorSchemeUnsafe: String?

    override val isOnline: Boolean
        get() = playerCache?.isOnline == true
    var logonBefore: Boolean = false
        internal set

    init {
        val userData = StorageManager.getUserData(uuid, initPlayer?.name)
        dbId = userData.dbId
        dbName = userData.name
        languageUnsafe = userData.language
        colorSchemeUnsafe = userData.colorScheme
    }

    override fun <T> kick(lang: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        player.syncTick {
            val msg = buildMiniMessage(lang, block, params = params)
            if (ServerInfo.isPaper)
                player.kick(msg.server)
            else
                @Suppress("DEPRECATION") // Spigot
                player.kickPlayer(msg.legacy)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerUser

        if (dbId != other.dbId) return false
        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dbId
        result = 31 * result + uuid.hashCode()
        return result
    }

    /************************
     #  Adventure Audience  #
     ************************/
    private val packCallbacks = ConcurrentHashMap<UUID, ResourcePackCallback>()

    fun sendBundle(packet: List<Packet<ClientCommonPacketListener>>) {
        player.handle.connection.send(ClientboundBundlePacket(packet))
    }

    override fun sendResourcePacks(request: ResourcePackRequest) {
        player.handle.connection ?: return
        if (request.replace()) {
            clearResourcePacks()
        }
        val prompt = request.prompt()?.let { ComponentSerializer.INSTANCE.toMinecraft(it) }
        val packs = buildList(request.packs().size) {
            for (pack in request.packs()) {
                add(ClientboundResourcePackPushPacket(
                    pack.id(), pack.uri().toASCIIString(), pack.hash(), request.required(),
                    if (size < request.packs().size) Optional.ofNullable(prompt) else Optional.empty()
                ))
                if (request.callback() != ResourcePackCallback.noOp()) {
                    packCallbacks[pack.id()] = request.callback()
                }
            }
        }
        sendBundle(packs)
    }

    override fun removeResourcePacks(id: UUID, vararg others: UUID) {
        player.handle.connection ?: return
        sendBundle(
            buildList(others.size + 1) {
                add(ClientboundResourcePackPopPacket(Optional.of(id)))
                addAll(others.map { ClientboundResourcePackPopPacket(Optional.of(it)) })
            }
        )
    }

    override fun clearResourcePacks() {
        player.handle.connection?.send(ClientboundResourcePackPopPacket(Optional.empty()))
    }

    @EventHandler // TODO: Life scope
    private fun onResourcePack(e: PlayerResourcePackStatusEvent) {
        val callback = when (e.status) {
            PlayerResourcePackStatusEvent.Status.ACCEPTED,
            PlayerResourcePackStatusEvent.Status.DOWNLOADED -> packCallbacks[e.id]
            else ->                                            packCallbacks.remove(e.id)
        }
        callback?.packEventReceived(e.id, ResourcePackStatus.valueOf(e.status.name), this)
    }

    override fun showDialog(dialog: DialogLike) {
        TODO()
//        player.handle.connection ?: return
//        player.handle.openDialog()
    }

    override fun closeDialog() {
        player.handle.connection?.send(ClientboundClearDialogPacket.INSTANCE)
    }

    override fun deleteMessage(signature: SignedMessage.Signature) {
        player.handle.connection ?: return
        val m = MessageSignature(signature.bytes())
        player.handle.connection.send(ClientboundDeleteChatPacket(MessageSignature.Packed(m)))
    }

    override fun sendMessage(message: Component, boundChatType: ChatType.Bound) {
        player.handle?.sendChatMessage(OutgoingChatMessage.Disguised(message.toMinecraft()), player.handle.isTextFilteringEnabled, boundChatType.toMinecraft())
    }

    override fun sendMessage(signedMessage: SignedMessage, boundChatType: ChatType.Bound) {
        player.handle.connection ?: return
        val msg = signedMessage.unsignedContent() ?: Component.text(signedMessage.message())
        if (signedMessage.isSystem) {
            sendMessage(msg, boundChatType)
        } else {
            super.sendMessage(signedMessage, boundChatType)
        }
    }

    override fun sendMessage(message: Component) {
        player.handle.connection?.send(ClientboundSystemChatPacket(message.toMinecraft(), false))
    }

    override fun sendActionBar(message: Component) {
        player.handle.connection?.send(ClientboundSetActionBarTextPacket(message.toMinecraft()))
    }

    override fun sendPlayerListHeader(header: Component) {
        // TODO: Confirm if should set playerListHeader field in CraftPlayer, both for Footer
        sendPlayerListHeaderAndFooter(header, Component.empty())
    }

    override fun sendPlayerListFooter(footer: Component) {
        sendPlayerListHeaderAndFooter(Component.empty(), footer)
    }

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
        player.handle.connection ?: return
        player.handle.connection.send(ClientboundTabListPacket(header.toMinecraft(), footer.toMinecraft()))
    }

    override fun showTitle(title: Title) {
        val connection = player.handle.connection ?: return
        title.times()?.let { times ->
            connection.send(ClientboundSetTitlesAnimationPacket(times.fadeIn().toTicks(), times.stay().toTicks(), times.fadeOut().toTicks()))
        }
        connection.send(ClientboundSetSubtitleTextPacket(title.subtitle().toMinecraft()))
        connection.send(ClientboundSetTitleTextPacket(title.title().toMinecraft()))
    }

    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T) {
        when (part) {
            TitlePart.TITLE -> player.handle.connection.send(ClientboundSetTitleTextPacket((value as Component).toMinecraft()))
            TitlePart.SUBTITLE -> player.handle.connection.send(ClientboundSetSubtitleTextPacket((value as Component).toMinecraft()))
            TitlePart.TIMES -> {
                val times = value as Title.Times
                player.handle.connection.send(ClientboundSetTitlesAnimationPacket(times.fadeIn().toTicks(), times.stay().toTicks(), times.fadeOut().toTicks()))
            }
            else -> throw IllegalArgumentException("Unknown titlePart: $part")
        }
    }

    private fun Duration?.toTicks(): Int {
        return if (this == null) -1 else (toMillis() / 50L).toInt()
    }

    override fun clearTitle() {
        player.handle.connection?.send(ClientboundClearTitlesPacket(false))
    }

    override fun resetTitle() {
        player.handle.connection?.send(ClientboundClearTitlesPacket(true))
    }

    // TODO: clear the set on player quit?
    private val activeBossBars by lazy { mutableSetOf<BossBar>() }

    override fun showBossBar(bar: BossBar) {
        BossBarImplementation.get(bar, BossBarImplementationImpl::class.java).show(player)
        activeBossBars.add(bar)
    }

    override fun hideBossBar(bar: BossBar) {
        BossBarImplementation.get(bar, BossBarImplementationImpl::class.java).hide(player)
        activeBossBars.remove(bar)
    }

    override fun playSound(sound: Sound) {
        val pos = player.handle.position()
        playSound(sound, pos.x, pos.y, pos.z)
    }

    override fun playSound(sound: Sound, x: Double, y: Double, z: Double) {
        playSound(sound, x, y, z, sound.seed().orElseGet { player.handle.random.nextLong() })
    }

    override fun playSound(sound: Sound, emitter: Sound.Emitter) {
        val entity = when (emitter) {
            Sound.Emitter.self() -> player
            is EsuBukkitEmitter  -> emitter.entity
            else                 -> throw IllegalArgumentException("Unknown emitter: $emitter")
        }
        playSound(sound, entity, sound.seed().orElseGet { player.handle.random.nextLong() })
    }

    override fun stopSound(sound: Sound) {
        player.handle.connection.send(ClientboundStopSoundPacket(
            Identifier.fromNamespaceAndPath(sound.name().namespace(), sound.name().value()),
            sound.source().toMinecraft(),
        ))
    }

    private fun playSound(sound: Sound, x: Double, y: Double, z: Double, seed: Long) {
        val name = Identifier.fromNamespaceAndPath(sound.name().namespace(), sound.name().value())
        val soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(name)
        val source = sound.source().toMinecraft()

        val hoilder = soundEvent.map { BuiltInRegistries.SOUND_EVENT.wrapAsHolder(it) }.orElseGet { Holder.direct(
            SoundEvent.createVariableRangeEvent(name)) }
        player.handle.connection?.send(ClientboundSoundPacket(hoilder, source, x, y, z, sound.volume(), sound.pitch(), seed))
    }

    private fun playSound(sound: Sound, emitter: Entity, seed: Long) {
        val name = Identifier.fromNamespaceAndPath(sound.name().namespace(), sound.name().value())
        val soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(name)
        val source = sound.source().toMinecraft()

        val hoilder = soundEvent.map { BuiltInRegistries.SOUND_EVENT.wrapAsHolder(it) }.orElseGet { Holder.direct(
            SoundEvent.createVariableRangeEvent(name)) }
        player.handle.connection?.send(ClientboundSoundEntityPacket(hoilder, source, emitter.handle, sound.volume(), sound.pitch(), seed))
    }

    override fun openBook(book: Book) {
        val item = net.minecraft.world.item.ItemStack(Items.WRITTEN_BOOK)
        item.set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            WrittenBookContent(
                book.title().toPlain().toFilterable(),
                book.author().toPlain(),
                0,
                book.pages().map { it.toMinecraft().toFilterable() },
                true
            )
        )
        val handle = player.handle
        handle.connection?.send(ClientboundBundlePacket(
            listOf(
                ClientboundSetPlayerInventoryPacket(handle.inventory.selectedSlot, item),
                ClientboundOpenBookPacket(InteractionHand.MAIN_HAND),
                ClientboundSetPlayerInventoryPacket(handle.inventory.selectedSlot, handle.inventory.selectedItem),
            )
        ))
    }

    private fun Component.toPlain(): String {
        val builder = StringBuilder()
        // TODO: render input component?
        ComponentFlattener.basic().flatten(this) { builder.append(it) }
        return builder.toString()
    }

    private fun <T: Any> T.toFilterable() = Filterable.passThrough(this)

}