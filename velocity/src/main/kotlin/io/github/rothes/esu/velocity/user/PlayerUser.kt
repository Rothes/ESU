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
import com.velocitypowered.api.proxy.Player
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.velocity.plugin
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

class PlayerUser(override val uuid: UUID, initPlayer: Player? = null): VelocityUser() {

    constructor(player: Player): this(player.uniqueId, player)

    var playerCache: Player? = initPlayer
        get() {
            val cache = field
            if (cache != null) {
                // Check if the instance is as it is.
                if (cache.isActive) {
                    return cache
                }
            }
            val get = plugin.server.getPlayer(uuid).getOrNull()
            if (get != null) {
                field = get
                return get
            }
            return cache
        }
        internal set
    val player: Player
        get() = playerCache ?: error("Player $uuid is not online and there's no cached instance!")
    override val commandSender: CommandSource
        get() = player
    override val dbId: Int
    override val name: String
        get() = nameUnsafe!!
    override val nameUnsafe: String?
        get() = playerCache?.username
    override val clientLocale: String
        get() = with(player.playerSettings.locale) { language + '_' + country.lowercase() }

    override var languageUnsafe: String?
    override var colorSchemeUnsafe: String?

    override val isOnline: Boolean
        get() = playerCache?.isActive == true

    init {
        val userData = StorageManager.getUserData(uuid)
        dbId = userData.dbId
        languageUnsafe = userData.language
        colorSchemeUnsafe = userData.colorScheme
    }

    private var waitingSettings: CountDownLatch? = CountDownLatch(1)

    fun awaitSettings(timeout: Long = 1000): Boolean {
        if (player.hasSentPlayerSettings()) {
            return true
        } else {
            val latch = waitingSettings ?: return true // Now settings sent?
            return try {
                latch.await(timeout, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                false
            }
        }
    }

    internal fun onSettingsReceived() {
        waitingSettings?.let {
            it.countDown()
            waitingSettings = null
        }
    }

    override fun <T> kick(lang: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        player.disconnect(buildMiniMessage(lang, block, *params).server)
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


}