package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v21_11

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.SoundPacketSender
import io.github.rothes.esu.lib.adventure.sound.Sound
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

object SoundPacketSenderImpl: SoundPacketSender {

    override fun playSound(player: Player, sound: Sound, x: Double, y: Double, z: Double, seed: Long) {
        val name = Identifier.fromNamespaceAndPath(sound.name().namespace(), sound.name().value())
        val soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(name)
        val source = sound.source().toMinecraft()
        val holder = soundEvent.map { BuiltInRegistries.SOUND_EVENT.wrapAsHolder(it) }
            .orElseGet { Holder.direct(SoundEvent.createVariableRangeEvent(name)) }
        player.handle.connection.send(ClientboundSoundPacket(holder, source, x, y, z, sound.volume(), sound.pitch(), seed))
    }

    override fun playSound(player: Player, sound: Sound, emitter: Entity, seed: Long) {
        val name = Identifier.fromNamespaceAndPath(sound.name().namespace(), sound.name().value())
        val soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(name)
        val source = sound.source().toMinecraft()
        val holder = soundEvent.map { BuiltInRegistries.SOUND_EVENT.wrapAsHolder(it) }
            .orElseGet { Holder.direct(SoundEvent.createVariableRangeEvent(name)) }
        player.handle.connection.send(ClientboundSoundEntityPacket(holder, source, emitter.handle, sound.volume(), sound.pitch(), seed))
    }

    override fun stopSound(player: Player, sound: Sound) {
        player.handle.connection.send(
            ClientboundStopSoundPacket(
                Identifier.fromNamespaceAndPath(sound.name().namespace(), sound.name().value()),
                sound.source().toMinecraft(),
            )
        )
    }

}