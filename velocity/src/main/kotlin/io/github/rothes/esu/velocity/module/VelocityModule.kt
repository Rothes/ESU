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

package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.velocity.plugin
import java.nio.file.Path

abstract class VelocityModule<C, L> : CommonModule<C, L>() {

    open val velocityPlugin: Any
        get() = plugin.bootstrap

    override val moduleFolder: Path
        get() = Path.of("plugins")
            .resolve(plugin.server.pluginManager.ensurePluginContainer(velocityPlugin).description.id)
            .resolve("modules").resolve(name)

    protected val registeredListeners = arrayListOf<Pair<Any, Any>>()

    override fun onDisable() {
        super.onDisable()
        for (listener in registeredListeners) {
            unregisterListener(listener.first, listener.second)
        }
        registeredListeners.clear()
    }

    fun registerListener(listener: Any, pluginInstance: Any = plugin.bootstrap) {
        plugin.server.eventManager.register(pluginInstance, listener)
        registeredListeners.add(listener to pluginInstance)
    }

    fun unregisterListener(listener: Any, pluginInstance: Any = plugin.bootstrap) {
        if (pluginInstance === plugin.bootstrap) {
            if (plugin.enabled) {
                plugin.server.eventManager.unregisterListener(pluginInstance, listener)
            }
        } else {
            plugin.server.eventManager.unregisterListener(pluginInstance, listener)
        }
    }

}