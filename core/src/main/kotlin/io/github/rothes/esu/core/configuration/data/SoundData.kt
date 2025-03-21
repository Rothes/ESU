package io.github.rothes.esu.core.configuration.data

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound

const val MINECRAFT = "minecraft"

data class SoundData(
    val namespace: String = MINECRAFT,
    val key: String = "",
    val source: Sound.Source? = null,
    val volume: Float? = null,
    val pitch: Float? = null,
    val seed: Long? = null,
) {

    val adventure by lazy {
        var sound = Sound.sound().type(Key.key(namespace, key))
        if (source != null) {
            sound = sound.source(source)
        }
        if (volume != null) {
            sound = sound.volume(volume)
        }
        if (pitch != null) {
            sound = sound.pitch(pitch)
        }
        if (seed != null) {
            sound = sound.seed(seed)
        }
        sound.build()
    }

}
