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

package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v19

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.SoundPacketSender
import io.github.rothes.esu.lib.adventure.sound.Sound
import net.minecraft.core.Registry
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

object SoundPacketSenderImpl: SoundPacketSender {

    override fun playSound(player: Player, sound: Sound, x: Double, y: Double, z: Double, seed: Long) {
        val name = ResourceLocation(sound.name().namespace(), sound.name().value())
        val soundEvent = Registry.SOUND_EVENT.getOptional(name)
        val source = sound.source().toMinecraft()
        if (soundEvent.isPresent) {
            player.handle.connection.send(ClientboundSoundPacket(soundEvent.get(), source, x, y, z, sound.volume(), sound.pitch(), seed))
        } else {
            player.handle.connection.send(ClientboundCustomSoundPacket(name, source, Vec3(x, y, z), sound.volume(), sound.pitch(), seed))
        }
    }

    override fun playSound(player: Player, sound: Sound, emitter: Entity, seed: Long) {
        val name = ResourceLocation(sound.name().namespace(), sound.name().value())
        val soundEvent = Registry.SOUND_EVENT.getOptional(name)
        val source = sound.source().toMinecraft()
        if (soundEvent.isPresent) {
            player.handle.connection.send(ClientboundSoundEntityPacket(soundEvent.get(), source, emitter.handle, sound.volume(), sound.pitch(), seed))
        } else {
            player.handle.connection.send(ClientboundCustomSoundPacket(name, source, emitter.handle.position(), sound.volume(), sound.pitch(), seed))
        }
    }

    override fun stopSound(player: Player, sound: Sound) {
        player.handle.connection.send(
            ClientboundStopSoundPacket(
                ResourceLocation(sound.name().namespace(), sound.name().value()),
                sound.source().toMinecraft(),
            )
        )
    }

}