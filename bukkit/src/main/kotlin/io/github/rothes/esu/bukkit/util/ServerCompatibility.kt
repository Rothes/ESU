package io.github.rothes.esu.bukkit.util

import org.bukkit.Location
import org.bukkit.entity.Entity

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