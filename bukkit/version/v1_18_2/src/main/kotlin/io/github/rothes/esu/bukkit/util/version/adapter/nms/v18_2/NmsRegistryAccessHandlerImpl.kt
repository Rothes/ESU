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

package io.github.rothes.esu.bukkit.util.version.adapter.nms.v18_2

import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer

object NmsRegistryAccessHandlerImpl: NmsRegistryAccessHandler {

    override fun getServerRegistryAccess(): RegistryAccess {
        return MinecraftServer.getServer().registryAccess() // Change: return type is RegistryAccess.Frozen
    }

    override fun <T: Any> getRegistryOrThrow(registryKey: ResourceKey<out Registry<T>>, registryAccess: RegistryAccess): Registry<T> {
        return registryAccess.registryOrThrow(registryKey)
    }

    override fun <T: Any> getNullable(registry: Registry<T>, key: ResourceKey<T>): T? {
        return registry.getOptional(key).orElse(null)
    }

    override fun <T: Any> getResourceKey(registry: Registry<T>, item: T): ResourceKey<T> {
        return registry.getResourceKey(item).orElseThrow()
    }

    override fun <T: Any> getId(registry: Registry<T>, item: T): Int {
        // Change: No need to cast, IdMap interface
        return registry.getId(item)
    }

    override fun <T: Any> entrySet(registry: Registry<T>): Set<Map.Entry<ResourceKey<T>, T>> = registry.entrySet()
    override fun <T: Any> keySet(registry: Registry<T>): Set<ResourceKey<T>> = registry.entrySet().map { it.key }.toSet()
    override fun <T: Any> values(registry: Registry<T>): Set<T> = registry.toSet()
    override fun <T: Any> size(registry: Registry<T>): Int = registry.size()

}