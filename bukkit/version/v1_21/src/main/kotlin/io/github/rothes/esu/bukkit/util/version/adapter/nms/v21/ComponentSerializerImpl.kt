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

package io.github.rothes.esu.bukkit.util.version.adapter.nms.v21

import com.google.gson.JsonParseException
import com.mojang.serialization.JsonOps
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ComponentSerializer
import io.github.rothes.esu.core.util.ComponentUtils
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.MinecraftServer
import io.github.rothes.esu.lib.adventure.text.Component as AdventureComponent
import net.minecraft.network.chat.Component as MinecraftComponent

// net.minecraft.network.chat.Component.Serializer exists in this version till 21.6, but we don't use
object ComponentSerializerImpl: ComponentSerializer {

    override fun toMinecraft(component: AdventureComponent): MinecraftComponent {
        return ComponentSerialization.CODEC.parse(
            // CraftRegistry.getMinecraftRegistry() can also work
            MinecraftServer.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE),
            ComponentUtils.gsonSerializer().serializeToTree(component),
        ).getOrThrow { JsonParseException(it) }
    }

    override fun toAdventure(component: MinecraftComponent): AdventureComponent {
        return ComponentUtils.gsonSerializer().deserializeFromTree(
            ComponentSerialization.CODEC.encodeStart(
                MinecraftServer.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE),
                component
            ).getOrThrow { JsonParseException(it) }
        )
    }
    
}