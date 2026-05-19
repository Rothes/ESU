/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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