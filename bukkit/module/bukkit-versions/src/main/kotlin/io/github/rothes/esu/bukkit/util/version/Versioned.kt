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

package io.github.rothes.esu.bukkit.util.version

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.core.util.version.Version
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Deprecated("> 0.15.0")
class Versioned<T, V>(
    target: Class<V>,
    type: String? = null,
    version: Version = ServerInfo.mcVersion,
): ReadOnlyProperty<T, V> {

    val handle = VersionedInstance.getVersioned(target, type, version)

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return handle
    }

}

@Deprecated("> 0.15.0")
object VersionedKt {
    @JvmStatic
    fun <T> Class<T>.versioned(): T {
        return VersionedInstance.getVersioned(this)
    }
}