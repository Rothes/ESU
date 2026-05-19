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

package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import java.util.*

class GenericUser(override val commandSender: CommandSender): BukkitUser() {

    override val dbId: Int
        get() = throw UnsupportedOperationException()
    override val uuid: UUID
        get() = throw UnsupportedOperationException()
    override val nameUnsafe: String
        get() = name
    override val clientLocale: String
        get() = EsuConfig.get().locale

    override var languageUnsafe: String? = null
    override var colorSchemeUnsafe: String? = null

    override val isOnline: Boolean = false

    override fun <T> kick(lang: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        throw UnsupportedOperationException("Cannot kick a GenericUser")
    }

}