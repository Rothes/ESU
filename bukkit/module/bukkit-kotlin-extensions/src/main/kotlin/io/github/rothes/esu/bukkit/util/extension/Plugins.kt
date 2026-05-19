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

package io.github.rothes.esu.bukkit.util.extension

import org.bukkit.plugin.Plugin
import java.lang.reflect.Proxy

fun Plugin.createChild(name: String = this.name, forceEnabled: Boolean = false): Plugin {
    return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Plugin::class.java)) { p, method, args ->
        when (method.name) {
            "isEnabled" -> forceEnabled || this.isEnabled
            "getName" -> name
            "hashCode" -> name.hashCode()
            "equals" -> p === args[0] || (args[0] is Plugin && name == (args[0] as Plugin).name)
            else -> {
                if (args == null)
                    method.invoke(this)
                else
                    method.invoke(this, args)
            }
        }
    } as Plugin
}