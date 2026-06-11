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

package io.github.rothes.esu.velocity.util.extension

import io.github.rothes.esu.velocity.core
import io.github.rothes.esu.velocity.plugin

fun VelocityListener.register(plugin: Any = io.github.rothes.esu.velocity.plugin) {
    core.server.eventManager.register(plugin, this)
}

fun VelocityListener.unregister() {
    core.server.eventManager.unregisterListener(plugin, this)
}

interface VelocityListener