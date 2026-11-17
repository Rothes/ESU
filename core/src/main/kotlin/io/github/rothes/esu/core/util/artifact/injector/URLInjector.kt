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

package io.github.rothes.esu.core.util.artifact.injector

import java.lang.reflect.InaccessibleObjectException
import java.net.URL

interface URLInjector {

    fun addURL(url: URL)

    companion object {

        private var _instance: URLInjector = UnsafeMethodURLInjector

        @JvmStatic
        fun loadUrl(url: URL) {
            try {
                _instance.addURL(url)
            } catch (e: InaccessibleObjectException) {
                _instance = when (_instance) {
                    UnsafeMethodURLInjector -> try {
                        UnsafeFieldURLInjector
                    } catch (_: Throwable) {
                        // On java 27: java.lang.NoSuchFieldException: path
                        ReflectURLInjector
                    }
                    UnsafeFieldURLInjector  -> ReflectURLInjector
                    else                    -> throw e
                }
                loadUrl(url)
            }
        }

    }

}