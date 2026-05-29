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

package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.lib.configurate.ConfigurationNode
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

data class LoadedConfiguration(
    val context: LoaderContext,
    val path: Path,
    val node: ConfigurationNode,
    val resourceNode: ConfigurationNode?,
) {

    fun save() {
        var root = node
        while (true) {
            val parent = node.parent() ?: break
            root = parent
        }
        context.loader.save(root)
    }

    fun saveIfNotEmpty() {
        var root = node
        while (true) {
            val parent = node.parent() ?: break
            root = parent
        }
        if (!root.empty()) {
            context.loader.save(root)
        }
    }

    fun <T> getAs(clazz: Class<T>): T {
        if (clazz.isInstance(EmptyConfiguration)) return clazz.cast(EmptyConfiguration)
        else if (clazz.isInstance(Unit)) return clazz.cast(Unit)

        if (context.mergeResources && resourceNode != null) {
            val defaultNode = context.loader.createNode()
            defaultNode.set(clazz.getConstructor().newInstance())
            mergeLang(defaultNode, resourceNode, node)
        }
        val instance = node.require(clazz)
        node.set(instance)
        return instance
    }

    fun node(vararg path: Any): LoadedConfiguration {
        return copy(
            node = this.node.node(*path),
            resourceNode = this.resourceNode?.node(*path),
        )
    }

    private fun mergeLang(def: ConfigurationNode, from: ConfigurationNode, to: ConfigurationNode) {
        if (def.isMap) {
            for (path in def.childrenMap().keys) {
                mergeLang(def.node(path), from.node(path), to.node(path))
            }
        } else {
            if (!from.virtual() && (to.virtual() || to.raw() == def.raw())) {
                to.raw(from.raw())
            }
        }
    }

    data class LoaderContext(
        val loader: YamlConfigurationLoader,
        val mergeResources: Boolean,
    )

}