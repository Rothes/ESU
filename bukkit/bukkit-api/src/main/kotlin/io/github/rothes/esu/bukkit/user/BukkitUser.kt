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

package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.audience
import io.github.rothes.esu.bukkit.configuration.data.ItemData
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

abstract class BukkitUser: User {

    private var _audience: Audience? = null
    private var _tagResolvers: List<TagResolver>? = null

    abstract override val commandSender: CommandSender
    override val audience: Audience
        get() = _audience ?: commandSender.audience.also { _audience = it }

    override fun getTagResolvers(): Iterable<TagResolver> {
        return _tagResolvers ?: User.DEFAULT_TAG_RESOLVERS.plus(ComponentBukkitUtils.papi(this)).also { _tagResolvers = it }
    }

    override var language: String?
        get() = languageUnsafe ?: clientLocale
        set(value) {
            languageUnsafe = value
        }
    override var colorScheme: String?
        get() = colorSchemeUnsafe
        set(value) {
            colorSchemeUnsafe = value
        }

    override val name: String
        get() = commandSender.name

    override fun hasPermission(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }

    fun <T: ConfigurationPart> item(locales: MultiLangConfiguration<T>, block: T.() -> ItemData?, vararg params: TagResolver): ItemStack {
        val itemData = lang(locales, block)
        return item(itemData, *params)
    }

    fun item(itemData: ItemData, vararg params: TagResolver): ItemStack {
        return itemData.parsed(this, *params)
    }

}