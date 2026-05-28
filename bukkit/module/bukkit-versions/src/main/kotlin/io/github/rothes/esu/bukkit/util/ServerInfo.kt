/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.core.util.extension.ClassUtils
import io.github.rothes.esu.core.util.version.Version
import io.github.rothes.esu.core.util.version.drop
import io.github.rothes.esu.core.util.version.toVersion
import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.Bukkit
import org.spigotmc.SpigotConfig

object ServerInfo {

    val mcVersion: Version = Bukkit.getServer().version
        .substringAfter("MC: ")
        .substringBefore(')')
        .substringBefore(' ') // For Spigot: CraftBukkit version 4598-Spigot-56165ca-d74c5d8 (MC: 1.21.11 Unobfuscated)
        .toVersion().let {
            if (it.major == 1) it.drop(1) else it // Remove "1." before 26.1 release
        }

    val isPaper: Boolean = ClassUtils.existsClass($$"com.destroystokyo.paper.VersionHistoryManager$VersionData")

    val isFolia: Boolean = ClassUtils.existsClass("io.papermc.paper.threadedregions.RegionizedServer")

    val isMojmap: Boolean
        get() = mcVersion >= 26 || try {
            val clazz = Class.forName("io.papermc.paper.util.MappingEnvironment")
            val method = clazz.getDeclaredMethod("reobf")
            method.isAccessible = true
            !(method.invoke(null) as Boolean)
        } catch (_: ClassNotFoundException) {
            false
        }

    val isProxyMode: Boolean
        get() = try {
            SpigotConfig.bungee || GlobalConfiguration.get().proxies.velocity.enabled
        } catch (_: NoSuchMethodException) {
            false
        } catch (_: NoClassDefFoundError) {
            false
        }

    val hasMojmap: Boolean
        get() = mcVersion >= "14.4" && mcVersion < "26"

}
