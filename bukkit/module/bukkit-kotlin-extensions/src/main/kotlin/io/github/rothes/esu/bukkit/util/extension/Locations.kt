package io.github.rothes.esu.bukkit.util.extension

import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.adventure.sound.Sound
import org.bukkit.Location

fun Location.playSound(sound: SoundData) {
    playSound(sound.adventure)
}

fun Location.playSound(sound: Sound) {
    playSound(sound.server)
}

fun Location.playSound(sound: net.kyori.adventure.sound.Sound) {
    world.playSound(sound, x, y, z)
}