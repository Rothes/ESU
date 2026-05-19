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

package io.github.rothes.esu.bukkit.module.esuchat

import io.github.rothes.esu.core.coroutine.IOScope
import io.github.rothes.esu.core.storage.StorageManager.database
import io.github.rothes.esu.core.storage.userId
import io.github.rothes.esu.core.user.User
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object EsuChatStorage {

    object EsuChatIgnoreTable : Table("esu_chat_ignore") {

        val user = userId("user")
        val ignoreSource = userId("ignore_source")
        val ignoreTime = datetime("ignore_time").defaultExpression(CurrentDateTime)

        init {
            uniqueIndex("uk_user__ignore_source", user, ignoreSource)

            transaction(database) {
                SchemaUtils.create(EsuChatIgnoreTable)
            }
        }
    }

    fun fetchIgnoreUsers(user: User) : IntSet {
        return transaction(database) {
            val result = EsuChatIgnoreTable.select(EsuChatIgnoreTable.ignoreSource)
                .where { EsuChatIgnoreTable.user eq user.dbId }
                .map { it[EsuChatIgnoreTable.ignoreSource] }

            IntOpenHashSet(result, 0.5f)
        }
    }

    fun addIgnore(user: User, ignoreSource: User) {
        IOScope.launch {
            transaction(database) {
                EsuChatIgnoreTable.insert {
                    it[EsuChatIgnoreTable.user] = user.dbId
                    it[EsuChatIgnoreTable.ignoreSource] = ignoreSource.dbId
                }
            }
        }
    }

    fun removeIgnore(user: User, ignoreSource: User) {
        IOScope.launch {
            transaction(database) {
                EsuChatIgnoreTable.deleteWhere {
                    (EsuChatIgnoreTable.user eq user.dbId) and (EsuChatIgnoreTable.ignoreSource eq ignoreSource.dbId)
                }
            }
        }
    }

}