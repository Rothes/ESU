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
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.clientVersionCode
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.displayName_
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.ObjectComponent
import io.github.rothes.esu.lib.adventure.text.format.NamedTextColor
import io.github.rothes.esu.lib.adventure.text.format.TextDecoration
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.Tag
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.Placeholder
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.adventure.text.`object`.ObjectContents
import io.github.rothes.esu.lib.adventure.text.`object`.PlayerHeadObjectContents
import me.clip.placeholderapi.PlaceholderAPIPlugin
import org.bukkit.entity.Player
import java.util.*

object ComponentBukkitUtils {

    private val PAPI_TAG_NAMES = setOf("placeholderapi", "papi")
    private val CONSOLE_HEAD = ObjectContents.playerHead().profileProperty(PlayerHeadObjectContents.property("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDBkNWE0ZWJhMTQwY2JiMzRkNDVlNDBkNjJkZDNmN2U2NTg0YjJhYjBiNDE1NWEwYmNjNjAzZDVkNzUwZjc5MyJ9fX0=")).build()

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

    fun playerHead(viewer: User, user: User, key: String = "player_head"): TagResolver.Single {
        playerHeadFallback(viewer, key)?.let { return it }
        return component(key, buildPlayerHead(if (user is PlayerUser) ObjectContents.playerHead().id(user.uuid).build() else CONSOLE_HEAD))
    }

    fun playerHead(viewer: User, player: UUID, key: String = "player_head"): TagResolver.Single {
        playerHeadFallback(viewer, key)?.let { return it }

        return component(key, buildPlayerHead(ObjectContents.playerHead().id(player).build()))
    }

    private fun playerHeadFallback(viewer: User, key: String): TagResolver.Single? {
        if (ServerInfo.mcVersion < "21.9" // Object component throws exception before Minecraft 1.21.9 (before it's added)
            || viewer !is PlayerUser // Object component will display "[unknown player head]" on console
            || viewer.player.clientVersionCode < 773 // Client >= 1.21.9 (773)
        ) return component(key, Component.empty())

        return null
    }

    private fun buildPlayerHead(contents: PlayerHeadObjectContents): ObjectComponent.Builder {
        return Component.`object`()
            .contents(contents)
            .decorate(TextDecoration.BOLD) // Expand right text offset
            .color(NamedTextColor.WHITE) // Clear color
    }

    fun papi(user: User): TagResolver {
        val player = if (user is PlayerUser) user.player else null
        return papi(player)
    }

    fun papi(player: Player?): TagResolver {
        return TagResolver.resolver(PAPI_TAG_NAMES) { arg, context ->
            val papi = arg.popOr("One argument expected for papi tag").value()
            if (ServerInfo.PluginEnabled.PlaceholderApi) {
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