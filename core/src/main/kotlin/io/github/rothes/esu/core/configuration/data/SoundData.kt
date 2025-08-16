package io.github.rothes.esu.core.configuration.data

import io.github.rothes.esu.lib.net.kyori.adventure.key.Key
import io.github.rothes.esu.lib.net.kyori.adventure.sound.Sound

const val MINECRAFT = "minecraft"

data class SoundData(
    val key: String = "",
    val source: Sound.Source? = null,
    val volume: Float? = null,
    val pitch: Float? = null,
    val seed: Long? = null,
) {

    val adventure by lazy {
        var sound = Sound.sound().type(Key.key(key))
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

    val serialized: String by lazy {
        if (seed != null)        "$key:${source?.name?.lowercase()}:$volume:$pitch:$seed"
        else if (pitch != null)  "$key:${source?.name?.lowercase()}:$volume:$pitch"
        else if (volume != null) "$key:${source?.name?.lowercase()}:$volume"
        else if (source != null) "$key:${source.name.lowercase()}"
        else key
    }

    companion object {

        fun parse(str: String): SoundData {
            val split = str.split(':')
            require(split.size >= 2) { "Failed to parse sound: At least namespace + key arguments required" }

            val namespace = split[0]
            val key = split[1]
            val source = if (split.size > 2) Sound.Source.valueOf(split[2].uppercase()) else null
            val volume = if (split.size > 3) split[3].toFloat() else null
            val pitch = if (split.size > 4) split[4].toFloat() else null
            val seed = if (split.size > 5) split[5].toLong() else null
            return SoundData("$namespace:$key", source, volume, pitch, seed)
        }

    }

}
