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

package io.github.rothes.esu.bukkit.util.version.adapter.nms.v19_3

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

object ResourceKeyHandlerImpl: ResourceKeyHandler {

    override fun <T: Any> parseResourceKey(registry: Registry<T>, id: String): ResourceKey<T> {
        val value = ResourceLocation.tryParse(id) ?: throw ResourceKeyHandler.BadIdentifierException(id)
        return ResourceKey.create(registry.key(), value)
    }

    override fun getResourceKeyString(resourceKey: ResourceKey<*>): String {
        val location = resourceKey.location()
        return if ((location.namespace == ResourceLocation.DEFAULT_NAMESPACE)) location.path
        else location.toString()
    }

}