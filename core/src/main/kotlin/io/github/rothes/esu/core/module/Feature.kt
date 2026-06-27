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

package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.configuration.LoadedConfiguration
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.EnableTogglable
import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.core.user.User

interface Feature<C, L> {

    val name: String
    val enabled: Boolean
    val parent: Feature<*, *>?
    val module: Module<*, *>

    val configClass: Class<C>
    val langClass: Class<L>

    val config: C
    val lang: MultiLangConfiguration<L>

    val permissionNode: String

    fun setConfigInstance(instance: C?) {}
    fun setLangInstance(instance: MultiLangConfiguration<L>?) {}
    fun setEnabled(value: Boolean) {
        // Notify children
        for (feature in getFeatures().let { if (enabled) it else it.reversed() }) {
            feature.toggleByAvailable()
        }
    }
    fun setParent(parent: Feature<*, *>?) {}

    fun toggleByAvailable(): AvailableCheck {
        val available = isAvailable()

        if (available.value && !enabled) {
            enableInternal()
        } else if (!available.value && enabled) {
            disableInternal()
        }

        return available
    }

    fun getFeatureMap(): Map<String, Feature<*, *>>
    fun getFeatures(): List<Feature<*, *>>
    fun getFeature(name: String): Feature<*, *>?
    fun registerFeature(child: Feature<*, *>)

    fun isAvailable(): AvailableCheck {
        return checkUnavailable() ?: AvailableCheck.OK
    }
    fun checkUnavailable(): AvailableCheck? {
        val config = config
        if (config is EnableTogglable && !config.enabled)
            return AvailableCheck.fail { "Feature not enabled".message }
        var parent = parent
        while (parent != null) {
            if (!parent.enabled) {
                return AvailableCheck.fail { "Parent ${parent!!.name} is not enabled".message }
            }
            parent = parent.parent
        }
        return null
    }

    fun onEnable()
    fun onDisable() {}
    fun onReload() {
        for (feature in getFeatures()) {
            feature.onReload()
        }
        toggleByAvailable()
    }

    fun onTerminate() {
        getFeatures().forEach { it.onTerminate() }
    }

    fun perm(shortPerm: String): String = "$permissionNode.${shortPerm.lowercase()}"

    fun preprocessBaseConfig(configuration: LoadedConfiguration) {}
    fun preprocessBaseLang(configuration: LoadedConfiguration) {}
    fun configNode(base: LoadedConfiguration): LoadedConfiguration = base.node(defaultNodePath())
    fun langNode(base: LoadedConfiguration): LoadedConfiguration = base.node(defaultNodePath())
    fun preprocessConfig(configuration: LoadedConfiguration) {}
    fun preprocessLang(configuration: LoadedConfiguration) {}

    private fun defaultNodePath(): String {
        val name = this.name
        val path = buildString(name.length + 4) {
            append(name.firstOrNull()?.lowercaseChar() ?: return@buildString)
            var i = 1
            while (i < name.length) {
                if (name[i].isUpperCase()) {
                    append('-')
                    append(name[i].lowercaseChar())
                } else {
                    append(name[i])
                }
                i++
            }
        }
        return path
    }

    class AvailableCheck(
        val value: Boolean,
        val messageBuilder: ((User) -> MessageData)?
    ) {

        companion object {
            val OK = AvailableCheck(true, null)
            fun fail(messageBuilder: (User) -> MessageData) = AvailableCheck(false, messageBuilder)
            fun Feature<*, *>.errFail(messageBuilder: (User) -> MessageData): AvailableCheck {
                LogUser.console.error(messageBuilder(LogUser.console), prefix = name)
                return AvailableCheck(false, messageBuilder)
            }
        }
    }

    private companion object {

        private fun Feature<*, *>.enableInternal() {
            try {
                onEnable()
                setEnabled(true)
            } catch (e: Throwable) {
                EsuBootstrap.instance.err("An exception occurred while enabling $name", e)
                disableInternal()
            }
        }

        private fun Feature<*, *>.disableInternal() {
            try {
                setEnabled(false)
                onDisable()
            } catch (e: Throwable) {
                EsuBootstrap.instance.err("An exception occurred while disabling $name", e)
            }
        }

    }

}