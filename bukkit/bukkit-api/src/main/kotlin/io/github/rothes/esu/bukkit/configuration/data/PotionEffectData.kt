package io.github.rothes.esu.bukkit.configuration.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeIf
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import java.time.Duration as JDuration

data class PotionEffectData(
    val type: String = "speed",
    val duration: JDuration = 1.seconds.toJavaDuration(),
    @NoDeserializeIf("1")
    val level: Int = 1,
    @NoDeserializeIf("true")
    val ambient: Boolean = true,
    @NoDeserializeIf("true")
    val particles: Boolean = true,
    @NoDeserializeIf("true")
    val icon: Boolean = true,
): ConfigurationPart {

    val bukkit
        get() = PotionEffect(
            PotionEffectType.getByKey(NamespacedKey.fromString(type)) ?: error("Unknown potion type: $type"),
            (duration.toMillis() / 50).toInt(),
            level - 1,
            ambient, particles, icon
        )
}
