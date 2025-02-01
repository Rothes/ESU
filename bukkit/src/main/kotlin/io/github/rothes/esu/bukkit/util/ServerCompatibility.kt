package io.github.rothes.esu.bukkit.util

import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.spigotmc.SpigotConfig

object ServerCompatibility {

    val folia = try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    val asyncTp = try {
        Entity::class.java.getMethod("teleportAsync", Location::class.java)
        true
    } catch (_: NoSuchMethodException) {
        false
    }

    val proxyMode = try {
        SpigotConfig.bungee || GlobalConfiguration.get().proxies.velocity.enabled
    } catch (_: NoSuchMethodException) {
        false
    } catch (_: NoClassDefFoundError) {
        false
    }

    fun Entity.tp(location: Location, then: ((Boolean) -> Unit)? = null) {
        if (asyncTp) {
            val future = teleportAsync(location)
            if (then != null) {
                future.thenAccept(then)
            }
        } else {
            val success = teleport(location)
            if (then != null) {
                then(success)
            }
        }
    }

}