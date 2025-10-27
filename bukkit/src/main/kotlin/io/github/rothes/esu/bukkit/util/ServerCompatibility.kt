package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.core.util.version.Version
import io.papermc.paper.configuration.GlobalConfiguration
import io.papermc.paper.util.MappingEnvironment
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.spigotmc.SpigotConfig

object ServerCompatibility {

    val serverVersion: Version = Version.fromString(Bukkit.getServer().bukkitVersion.split('-')[0])

    val isPaper = try {
        Class.forName($$"com.destroystokyo.paper.VersionHistoryManager$VersionData")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    val isFolia = try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    val isMojmap = try {
        !MappingEnvironment.reobf()
    } catch (_: NoClassDefFoundError) {
        false
    }

    val isProxyMode = try {
        SpigotConfig.bungee || GlobalConfiguration.get().proxies.velocity.enabled
    } catch (_: NoSuchMethodException) {
        false
    } catch (_: NoClassDefFoundError) {
        false
    }

    val hasMojmap = serverVersion >= Version.fromString("1.14.4")

    val asyncTp = try {
        Entity::class.java.getMethod("teleportAsync", Location::class.java)
        true
    } catch (_: NoSuchMethodException) {
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