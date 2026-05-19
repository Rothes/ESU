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
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey

interface NmsRegistryAccessHandler {

    fun getServerRegistryAccess(): RegistryAccess

    fun <T: Any> getRegistryOrThrow(registryKey: ResourceKey<out Registry<T>>, registryAccess: RegistryAccess = getServerRegistryAccess()): Registry<T>

    fun <T: Any> getNullable(registry: Registry<T>, key: ResourceKey<T>): T?

    fun <T: Any> getResourceKey(registry: Registry<T>, item: T): ResourceKey<T>
    fun <T: Any> getId(registry: Registry<T>, item: T): Int

    fun <T: Any> entrySet(registry: Registry<T>): Set<Map.Entry<ResourceKey<T>, T>>
    fun <T: Any> keySet(registry: Registry<T>): Set<ResourceKey<T>>
    fun <T: Any> values(registry: Registry<T>): Set<T>
    fun <T: Any> size(registry: Registry<T>): Int

}