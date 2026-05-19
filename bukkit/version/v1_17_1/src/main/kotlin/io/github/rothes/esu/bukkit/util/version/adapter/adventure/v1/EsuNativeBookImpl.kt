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

package io.github.rothes.esu.bukkit.util.version.adapter.adventure.v1

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.EsuNativeBook
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.world.item.ItemStack
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
object EsuNativeBookImpl: EsuNativeBook<Any> {

    override fun isSupported(): Boolean {
        return false
    }

    override fun createBook(title: String, author: String, pages: Iterable<Any?>): ItemStack? {
        TODO("Not yet implemented")
    }

    override fun openBook(viewer: Player, book: ItemStack) {
        TODO("Not yet implemented")
    }

    override fun createMessage(viewer: Player, message: Component): Any? {
        TODO("Not yet implemented")
    }
}