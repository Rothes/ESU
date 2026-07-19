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

package io.github.rothes.esu.bukkit.util.version.adapter.adventure

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ComponentSerializer
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import net.minecraft.sounds.SoundSource
import net.minecraft.world.BossEvent
import java.util.*
import io.github.rothes.esu.lib.adventure.bossbar.BossBar as AdventureBossBar
import io.github.rothes.esu.lib.adventure.chat.ChatType as AdventureChatType
import io.github.rothes.esu.lib.adventure.sound.Sound as AdventureSound
import io.github.rothes.esu.lib.adventure.text.Component as AdventureComponent
import net.minecraft.network.chat.ChatType as MinecraftChatType
import net.minecraft.network.chat.Component as MinecraftComponent

object AdventureConverter {

    private val CHAT_TYPE_REGISTRY = versioned<NmsRegistryAccessHandler>().getRegistryOrThrow(versioned<NmsRegistries>().chatType)
    private val RESOURCE_KEY_HANDLER = versioned<ResourceKeyHandler>()

    fun AdventureChatType.Bound.toMinecraft(): MinecraftChatType.Bound {
        val key = type().key()!!
        return MinecraftChatType.Bound(
            CHAT_TYPE_REGISTRY.getOrThrow(RESOURCE_KEY_HANDLER.createResourceKey(CHAT_TYPE_REGISTRY, key.namespace(), key.value())),
            ComponentSerializer.INSTANCE.toMinecraft(name()),
            Optional.ofNullable(target()?.let { ComponentSerializer.INSTANCE.toMinecraft(it) })
        )
    }

    fun AdventureComponent.toMinecraft(): MinecraftComponent {
        return ComponentSerializer.INSTANCE.toMinecraft(this)
    }

    fun AdventureBossBar.Color.toMinecraft(): BossEvent.BossBarColor {
        return when (this) {
            AdventureBossBar.Color.PINK -> BossEvent.BossBarColor.PINK
            AdventureBossBar.Color.BLUE -> BossEvent.BossBarColor.BLUE
            AdventureBossBar.Color.RED -> BossEvent.BossBarColor.RED
            AdventureBossBar.Color.GREEN -> BossEvent.BossBarColor.GREEN
            AdventureBossBar.Color.YELLOW -> BossEvent.BossBarColor.YELLOW
            AdventureBossBar.Color.PURPLE -> BossEvent.BossBarColor.PURPLE
            AdventureBossBar.Color.WHITE -> BossEvent.BossBarColor.WHITE
        }
    }

    fun AdventureBossBar.Overlay.toMinecraft(): BossEvent.BossBarOverlay {
        return when (this) {
            AdventureBossBar.Overlay.PROGRESS -> BossEvent.BossBarOverlay.PROGRESS
            AdventureBossBar.Overlay.NOTCHED_6 -> BossEvent.BossBarOverlay.NOTCHED_6
            AdventureBossBar.Overlay.NOTCHED_10 -> BossEvent.BossBarOverlay.NOTCHED_10
            AdventureBossBar.Overlay.NOTCHED_12 -> BossEvent.BossBarOverlay.NOTCHED_12
            AdventureBossBar.Overlay.NOTCHED_20 -> BossEvent.BossBarOverlay.NOTCHED_20
        }
    }

    fun AdventureSound.Source.toMinecraft(): SoundSource {
        return when (this) {
            AdventureSound.Source.MASTER -> SoundSource.MASTER
            AdventureSound.Source.MUSIC -> SoundSource.MUSIC
            AdventureSound.Source.RECORD -> SoundSource.RECORDS
            AdventureSound.Source.WEATHER -> SoundSource.WEATHER
            AdventureSound.Source.BLOCK -> SoundSource.BLOCKS
            AdventureSound.Source.HOSTILE -> SoundSource.HOSTILE
            AdventureSound.Source.NEUTRAL -> SoundSource.NEUTRAL
            AdventureSound.Source.PLAYER -> SoundSource.PLAYERS
            AdventureSound.Source.AMBIENT -> SoundSource.AMBIENT
            AdventureSound.Source.VOICE -> SoundSource.VOICE
            AdventureSound.Source.UI -> SoundSource.UI
        }
    }

}