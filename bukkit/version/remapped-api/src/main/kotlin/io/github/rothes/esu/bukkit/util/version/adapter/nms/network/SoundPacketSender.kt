package io.github.rothes.esu.bukkit.util.version.adapter.nms.network

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.lib.adventure.sound.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

interface SoundPacketSender {

    fun playSound(player: Player, sound: Sound, x: Double, y: Double, z: Double, seed: Long = sound.seed().orElseGet { player.handle.random.nextLong() })
    fun playSound(player: Player, sound: Sound, emitter: Entity, seed: Long = sound.seed().orElseGet { player.handle.random.nextLong() })
    fun stopSound(player: Player, sound: Sound)

    companion object {

        val INSTANCE = versioned<SoundPacketSender>()

    }

}