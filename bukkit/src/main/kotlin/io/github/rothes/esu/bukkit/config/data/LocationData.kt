package io.github.rothes.esu.bukkit.config.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import org.bukkit.Bukkit
import org.bukkit.Location

data class LocationData(
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f,
): ConfigurationPart {

    val bukkit: Location
        get() = Location(Bukkit.getWorld(world), x, y, z, yaw, pitch)

}
