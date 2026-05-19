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

package io.github.rothes.esu.core.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class InitOnce<T, V>: ReadWriteProperty<T, V> {

    private var value: V? = null

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return value ?: throw IllegalStateException("${property.name} has not been initialized")
    }

    override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        if (this.value != null) {
            throw IllegalStateException("${property.name} is already initialized")
        }
        this.value = value
    }

}