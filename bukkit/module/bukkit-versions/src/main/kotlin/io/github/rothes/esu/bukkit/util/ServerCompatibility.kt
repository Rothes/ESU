package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.core.util.version.Version
import io.github.rothes.esu.core.util.version.drop
import io.github.rothes.esu.core.util.version.toVersion
import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.spigotmc.SpigotConfig

object ServerCompatibility {

    val serverVersion: Version = Bukkit.getServer().version
        .substringAfter("MC: ")
        .substringBefore(')')
        .substringBefore(' ') // For Spigot: CraftBukkit version 4598-Spigot-56165ca-d74c5d8 (MC: 1.21.11 Unobfuscated)
        .toVersion().let {
            if (it.major == 1) it.drop(1) else it // Remove "1." before 26.1 release
        }

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

    val isMojmap = serverVersion >= 26 || try {
        val clazz = Class.forName("io.papermc.paper.util.MappingEnvironment")
        val method = clazz.getDeclaredMethod("reobf")
        method.isAccessible = true
        !(method.invoke(null) as Boolean)
    } catch (_: ClassNotFoundException) {
        false
    }

    val isProxyMode = try {
        SpigotConfig.bungee || GlobalConfiguration.get().proxies.velocity.enabled
    } catch (_: NoSuchMethodException) {
        false
    } catch (_: NoClassDefFoundError) {
        false
    }

    val hasMojmap = serverVersion >= "14.4" && serverVersion < "26"

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