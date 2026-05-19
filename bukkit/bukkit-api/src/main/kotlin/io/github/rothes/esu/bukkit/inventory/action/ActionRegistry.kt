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

package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.core
import java.lang.reflect.Modifier

object ActionRegistry {

    private val registry = hashMapOf<String, Action>()

    init {
        CommonActions::class.java.declaredFields.filter {
            Action::class.java.isAssignableFrom(it.type) && it.modifiers and Modifier.STATIC != 0
        }.forEach {
            it.isAccessible = true
            register(it.get(null) as Action)
        }
    }

    fun register(vararg actions: Action) {
        actions.forEach { register(action = it) }
    }

    fun register(action: Action): Boolean {
        if (registry.containsKey(action.name)) {
            return false
        }
        registry[action.id.lowercase()] = action
        return true
    }

    fun unregister(action: Action): Boolean {
        return registry.remove(action.id, action)
    }

    fun parseActions(string: List<String>): List<ParsedAction> {
        return string.mapNotNull { parseAction(it) }
    }

    fun parseAction(string: String?): ParsedAction? {
        string ?: return null
        if (string.isEmpty() || string.first() != '[')
            return null
        val index = string.indexOf(']')
        if (index == -1)
            return null
        val name = string.substring(1, index).lowercase()
        val action = registry[name]
        if (action == null) {
            core.warn("Unknown action '$string'")
            return null
        }
        return ParsedAction(action, string.substring(index + 1).removePrefix(" ").ifEmpty { null })
    }

}