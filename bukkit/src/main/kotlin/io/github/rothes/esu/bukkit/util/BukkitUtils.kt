package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.net.kyori.adventure.sound.Sound
import org.bukkit.Location

object BukkitUtils {

    fun Location.playSound(sound: SoundData) {
        playSound(sound.adventure)
    }
    fun Location.playSound(sound: Sound) {
        playSound(sound.server)
    }
    fun Location.playSound(sound: net.kyori.adventure.sound.Sound) {
        world.playSound(sound, x, y, z)
    }

}