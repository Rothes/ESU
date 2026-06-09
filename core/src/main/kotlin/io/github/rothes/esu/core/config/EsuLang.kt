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

package io.github.rothes.esu.core.config

import io.github.rothes.esu.core.config.EsuLang.BaseEsuLangData
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.caption.Caption

abstract class EsuLang<T: BaseEsuLangData> {

    private var data: MultiLangConfiguration<T> = load()

    fun get() = data

    fun reloadConfig() {
        data = load()
    }

    protected abstract fun load(): MultiLangConfiguration<T>

    open class BaseEsuLangData(
        val commands: Commands = Commands(),
        val updater: Updater = Updater(),
        val booleans: Booleans = Booleans(),
        val format: Format = Format(),
        val commandCaptions: LinkedHashMap<Caption, String> = LinkedHashMap(),
    ): ConfigurationPart {

        data class Commands(
            val module: Module = Module(),
            val reload: Reload = Reload(),
        ) {
            data class Module(
                val forceEnable: ForceEnable = ForceEnable(),
                val forceDisable: ForceDisable = ForceDisable(),
            ) {
                data class ForceEnable(
                    val moduleEnabled: MessageData = "<pc>Module <pdc><module-name> <pc>is enabled now.".message,
                    val alreadyEnabled: MessageData = "<ec>Module <edc><module-name> <ec>is already enabled.".message,
                    val failedEnable: MessageData = "<ec>Failed to enable module <edc><module-name><ec>.".message,
                )
                data class ForceDisable(
                    val moduleDisabled: MessageData = "<pc>Module <pdc><module-name> <pc>is disabled now.".message,
                    val alreadyDisabled: MessageData = "<ec>Module <edc><module-name> <ec>is already disabled.".message,
                    val failedDisable: MessageData = "<ec>Failed to disable module <edc><module-name><ec>.".message,
                )
            }

            data class Reload(
                val complete: MessageData = "<pc>Reloaded global & module configs.".message,
            )
        }

        data class Updater(
            val checker: Checker = Checker(),
        ) {
            data class Checker(
                val networkError: String = "<ec>Failed to check for update: <message>",
                val unknownChannel: String = "<ec>Failed to check for update, unknown current channel: <channel>",
                val unknownPlatform: String = "<ec>Failed to check for update, unknown current platform: <platform>",
            )
        }

        data class Booleans(
            val enabled: String = "enabled",
            val disabled: String = "disabled",
            val on: String = "on",
            val off: String = "off",
            val yes: String = "yes",
            val no: String = "no",
        ): ConfigurationPart

        data class Format(
            val duration: Duration = Duration(),
        ): ConfigurationPart {
            data class Duration(
                val day: String = "<value>d",
                val hour: String = "<value>h",
                val minute: String = "<value>m",
                val second: String = "<value>s",
                val millis: String = "<value>ms",
                val separator: String = " ",
            ): ConfigurationPart
        }
    }

    companion object {
        var instance: EsuLang<out BaseEsuLangData> by InitOnce()

        fun get(): MultiLangConfiguration<out BaseEsuLangData> = instance.data
    }

}