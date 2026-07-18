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

package io.github.rothes.esu.common.util.coroutine

import io.github.rothes.esu.core.EsuBootstrap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlin.concurrent.thread

object CoroutineLife {

    fun shutdown() {
        thread(name = "Esu-Coroutine Shutdown Dispatcher #${EsuBootstrap.instance.hashCode()}", isDaemon = true) {
            try {
                @OptIn(DelicateCoroutinesApi::class) // Just release the resources
                Dispatchers.shutdown()
            } catch (t: Throwable) {
                EsuBootstrap.instance.warn("An exception occurred while shutting down coroutine: $t")
            }
        }
    }

}