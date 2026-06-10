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

package io.github.rothes.esu.core

import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.CommandManager
import java.nio.file.Path

interface EsuCore : EsuLogger {

    val initialized: Boolean
    val commandManager: CommandManager<User>
    val baseCommandNode: String

    val baseConfigPath: Path
        get() = EsuBootstrap.instance.baseConfigPath

    override fun info(message: String) {
        EsuBootstrap.instance.info(message)
    }
    override fun warn(message: String) {
        EsuBootstrap.instance.warn(message)
    }
    override fun err(message: String) {
        EsuBootstrap.instance.err(message)
    }
    override fun err(message: String, throwable: Throwable?) {
        EsuBootstrap.instance.err(message, throwable)
    }

    companion object {

        var instance: EsuCore by InitOnce()

    }

}