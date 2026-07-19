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
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.BossBarImplementationImpl
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.BukkitEmitter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.*
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
import io.github.rothes.esu.lib.adventure.pointer.Pointers
import io.github.rothes.esu.lib.adventure.resource.ResourcePackCallback
import io.github.rothes.esu.lib.adventure.resource.ResourcePackRequest
import io.github.rothes.esu.lib.adventure.resource.ResourcePackStatus
import io.github.rothes.esu.lib.adventure.sound.Sound
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.adventure.title.Title
import io.github.rothes.esu.lib.adventure.title.TitlePart
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
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

    override fun sendResourcePacks(request: ResourcePackRequest) {
        if (request.replace()) clearResourcePacks()
        for (pack in request.packs()) {
            if (request.callback() != ResourcePackCallback.noOp()) {
                packCallbacks[pack.id()] = request.callback()
            }
        }
        ResourcePackPacketSender.INSTANCE.sendResourcePacks(player, request)
    }

    override fun removeResourcePacks(id: UUID, vararg others: UUID) {
        ResourcePackPacketSender.INSTANCE.removeResourcePacks(player, id, others = others)
    }

    override fun clearResourcePacks() {
        ResourcePackPacketSender.INSTANCE.clearResourcePacks(player)
    }

    fun handleResourcePackStatus(e: PlayerResourcePackStatusEvent) {
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
        // TODO
        player.handle.connection.send(ClientboundClearDialogPacket.INSTANCE)
    }

    override fun deleteMessage(signature: SignedMessage.Signature) {
        ChatMessagePacketSender.INSTANCE.deleteMessage(player, signature)
    }

    override fun sendMessage(message: Component, boundChatType: ChatType.Bound) {
        ChatMessagePacketSender.INSTANCE.sendMessage(player, message, boundChatType)
    }

    override fun sendMessage(signedMessage: SignedMessage, boundChatType: ChatType.Bound) {
        val msg = signedMessage.unsignedContent() ?: Component.text(signedMessage.message())
        if (signedMessage.isSystem) {
            ChatMessagePacketSender.INSTANCE.sendMessage(player, msg, boundChatType)
        } else {
            super.sendMessage(signedMessage, boundChatType)
        }
    }

    override fun sendMessage(message: Component) {
        ChatMessagePacketSender.INSTANCE.sendSystemMessage(player, message)
    }

    override fun sendActionBar(message: Component) {
        TitlePacketSender.INSTANCE.sendActionBar(player, message)
    }

    override fun sendPlayerListHeader(header: Component) {
        sendPlayerListHeaderAndFooter(header, Component.empty())
    }

    override fun sendPlayerListFooter(footer: Component) {
        sendPlayerListHeaderAndFooter(Component.empty(), footer)
    }

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
        TabListPacketSender.INSTANCE.sendPlayerListHeaderAndFooter(player, header, footer)
    }

    override fun showTitle(title: Title) {
        title.times()?.let { times ->
            TitlePacketSender.INSTANCE.sendTitlesAnimation(player, times.fadeIn().toTicks(), times.stay().toTicks(), times.fadeOut().toTicks())
        }
        TitlePacketSender.INSTANCE.sendSubtitle(player, title.subtitle())
        TitlePacketSender.INSTANCE.sendTitle(player, title.title())
    }

    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T) {
        when (part) {
            TitlePart.TITLE -> TitlePacketSender.INSTANCE.sendTitle(player, value as Component)
            TitlePart.SUBTITLE -> TitlePacketSender.INSTANCE.sendSubtitle(player, value as Component)
            TitlePart.TIMES -> {
                val times = value as Title.Times
                TitlePacketSender.INSTANCE.sendTitlesAnimation(player, times.fadeIn().toTicks(), times.stay().toTicks(), times.fadeOut().toTicks())
            }
            else -> throw IllegalArgumentException("Unknown titlePart: $part")
        }
    }

    private fun Duration?.toTicks(): Int {
        return if (this == null) -1 else (toMillis() / 50L).toInt()
    }

    override fun clearTitle() {
        TitlePacketSender.INSTANCE.clearTitle(player)
    }

    override fun resetTitle() {
        TitlePacketSender.INSTANCE.resetTitle(player)
    }

    private val activeBossBars by lazy { mutableSetOf<BossBar>() }

    override fun showBossBar(bar: BossBar) {
        BossBarImplementation.get(bar, BossBarImplementationImpl::class.java).show(player)
        activeBossBars.add(bar)
    }

    override fun hideBossBar(bar: BossBar) {
        BossBarImplementation.get(bar, BossBarImplementationImpl::class.java).hide(player)
        activeBossBars.remove(bar)
    }

    fun clearBossBars() {
        for (bar in activeBossBars) {
            BossBarImplementation.get(bar, BossBarImplementationImpl::class.java).hide(player)
        }
        activeBossBars.clear()
    }

    override fun playSound(sound: Sound) {
        val pos = player.handle.position()
        playSound(sound, pos.x, pos.y, pos.z)
    }

    override fun playSound(sound: Sound, x: Double, y: Double, z: Double) {
        SoundPacketSender.INSTANCE.playSound(player, sound, x, y, z)
    }

    override fun playSound(sound: Sound, emitter: Sound.Emitter) {
        val entity = when (emitter) {
            Sound.Emitter.self() -> player
            is BukkitEmitter     -> emitter.entity
            else                 -> throw IllegalArgumentException("Unknown emitter: $emitter")
        }
        SoundPacketSender.INSTANCE.playSound(player, sound, entity)
    }

    override fun stopSound(sound: Sound) {
        SoundPacketSender.INSTANCE.stopSound(player, sound)
    }

    override fun openBook(book: Book) {
        OpenBookPacketSender.INSTANCE.openBook(player, book)
    }

    override fun pointers(): Pointers {
        return super.pointers()
    }

}