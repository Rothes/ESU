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

package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.bukkit.user.GenericUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.displayName_
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.Tag
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.Placeholder
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import me.clip.placeholderapi.PlaceholderAPIPlugin
import org.bukkit.entity.Player

object ComponentBukkitUtils {

    private val PAPI_TAG_NAMES = setOf("placeholderapi", "papi")

    fun player(player: Player, key: String = "player"): TagResolver.Single {
        return Placeholder.component(key, player.displayName_)
    }

    fun user(user: User, key: String = "player"): TagResolver.Single {
        return when (user) {
            is PlayerUser -> player(user.player, key)
            is GenericUser -> component(key, user.commandSender.name().esu)
            else -> unparsed(key, user.name)
        }
    }

    fun papi(user: User): TagResolver {
        val player = if (user is PlayerUser) user.player else null
        return papi(player)
    }

    fun papi(player: Player?): TagResolver {
        return TagResolver.resolver(PAPI_TAG_NAMES) { arg, context ->
            val papi = arg.popOr("One argument expected for papi tag").value()
            if (ServerInfo.PluginDependency.hasPlaceholderApi) {
                val split = papi.split('_', limit = 2)
                val expansion = PlaceholderAPIPlugin.getInstance().localExpansionManager.getExpansion(split[0].lowercase())
                    ?: return@resolver Tag.inserting(Component.text(papi))
                val result = expansion.onRequest(player, split.getOrElse(1) { "" }) ?: papi

                val type = if (arg.hasNext()) arg.pop().lowerValue() else "plain"
                when (type) {
                    "plain" -> Tag.inserting(Component.text(result))
                    "legacy" -> Tag.inserting(result.legacy)
                    "minimessage" -> Tag.inserting(context.deserialize(result))
                    else -> error("Unknown text type $type")
                }

            } else {
                Tag.inserting(Component.text(papi))
            }
        }
    }

}