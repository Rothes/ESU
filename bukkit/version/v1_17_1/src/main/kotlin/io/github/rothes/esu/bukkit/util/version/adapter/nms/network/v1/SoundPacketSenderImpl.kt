package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v1

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
            player.handle.connection.send(ClientboundSoundPacket(soundEvent.get(), source, x, y, z, sound.volume(), sound.pitch()))
        } else {
            player.handle.connection.send(ClientboundCustomSoundPacket(name, source, Vec3(x, y, z), sound.volume(), sound.pitch()))
        }
    }

    override fun playSound(player: Player, sound: Sound, emitter: Entity, seed: Long) {
        val name = ResourceLocation(sound.name().namespace(), sound.name().value())
        val soundEvent = Registry.SOUND_EVENT.getOptional(name)
        val source = sound.source().toMinecraft()
        if (soundEvent.isPresent) {
            player.handle.connection.send(ClientboundSoundEntityPacket(soundEvent.get(), source, emitter.handle, sound.volume(), sound.pitch()))
        } else {
            player.handle.connection.send(ClientboundCustomSoundPacket(name, source, emitter.handle.position(), sound.volume(), sound.pitch()))
        }
    }

    override fun stopSound(player: Player, sound: Sound) {
        player.handle.connection.send(ClientboundStopSoundPacket(
            ResourceLocation(sound.name().namespace(), sound.name().value()),
            sound.source().toMinecraft(),
        ))
    }

}