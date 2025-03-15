package io.github.rothes.esu.bukkit.config.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeIf
import org.bukkit.Bukkit
import org.bukkit.Location

data class LocationData(
    val world: String? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    @NoDeserializeIf("0.0")
    val yaw: Float = 0.0f,
    @NoDeserializeIf("0.0")
    val pitch: Float = 0.0f,
): ConfigurationPart {

    val bukkit: Location
        get() = Location(world?.let { Bukkit.getWorld(it) }, x, y, z, yaw, pitch)

}
