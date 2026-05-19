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

package io.github.rothes.esu.bukkit.util.version.adapter.nms.v20

import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.PlayerEntityVisibilityHandler
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import io.github.rothes.esu.core.util.UnsafeUtils.usNullableObjAccessor
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.ref.WeakReference
import java.util.*

object PlayerEntityVisibilityHandlerImpl : PlayerEntityVisibilityHandler {

    private val hiddenEntities = CraftPlayer::class.java.getDeclaredField("invertedVisibilityEntities").usNullableObjAccessor
    private val pluginWeakReferences = CraftPlayer::class.java.getDeclaredMethod("getPluginWeakReference", Plugin::class.java).handle
    private val entityHandleGetter by Versioned(EntityHandleGetter::class.java)

    override fun forceShowEntity(player: Player, bukkitEntity: Entity, plugin: Plugin) {
        @Suppress("UNCHECKED_CAST")
        val map = hiddenEntities[player] as MutableMap<UUID, MutableSet<WeakReference<Plugin>>>
        val entity = entityHandleGetter.getHandle(bukkitEntity)
        val uuid = entity.uuid
        val set = map[uuid]
        if (entity.visibleByDefault) {
            set ?: return
            val pluginReference = pluginWeakReferences.invokeExact(plugin) as WeakReference<*>
            if (!set.remove(pluginReference)) return
            if (set.isEmpty()) map.remove(uuid)
        } else {
            @Suppress("UNCHECKED_CAST")
            val pluginReference = pluginWeakReferences.invokeExact(plugin) as WeakReference<Plugin>
            if (set != null)
                set.add(pluginReference)
            else
                map[uuid] = hashSetOf(pluginReference)
        }
    }

}