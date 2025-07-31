package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.bukkit.plugin
import io.papermc.paper.configuration.GlobalConfiguration
import io.papermc.paper.util.MappingEnvironment
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.spigotmc.SpigotConfig

object ServerCompatibility {

    val paper = try {
        Class.forName("com.destroystokyo.paper.VersionHistoryManager\$VersionData")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    val folia = try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    val mojmap = try {
        !MappingEnvironment.reobf()
    } catch (_: NoClassDefFoundError) {
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

    object CB {
        val adventure = BukkitAudiences.create(plugin)
    }

}