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

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.user.ConsoleConst
import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.slf4j.Logger
import java.util.*

object ConsoleUser: BukkitUser(), LogUser {

    override val commandSender: CommandSender = Bukkit.getConsoleSender()
    override val dbId: Int
    override val dbName: String?
    override val name: String = ConsoleConst.NAME
    override val nameUnsafe: String = name
    override val clientLocale: String
        get() = EsuConfig.get().locale
    override val uuid: UUID = ConsoleConst.UUID

    override var languageUnsafe: String?
    override var colorSchemeUnsafe: String?

    override val isOnline: Boolean = true

    override val logger: Logger = org.slf4j.LoggerFactory.getLogger(plugin.logger.name)

    init {
        val userData = StorageManager.getConsoleUserData()
        dbId = userData.dbId
        dbName = userData.name
        languageUnsafe = userData.language
        colorSchemeUnsafe = userData.colorScheme

        LogUser.console = this
    }

    override fun <T> kick(lang: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        throw UnsupportedOperationException("Cannot kick a ConsoleUser")
    }

    override fun print(message: String) {
        commandSender.sendMessage(message)
    }

}