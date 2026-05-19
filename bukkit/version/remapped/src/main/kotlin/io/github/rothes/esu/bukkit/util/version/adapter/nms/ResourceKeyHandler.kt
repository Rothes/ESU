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

package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

interface ResourceKeyHandler {

    fun <T: Any> parseResourceKey(registry: Registry<T>, id: String): ResourceKey<T>
    fun getResourceKeyString(resourceKey: ResourceKey<*>): String

    class BadIdentifierException(val id: String): RuntimeException("Failed to parse identifier: $id")

}