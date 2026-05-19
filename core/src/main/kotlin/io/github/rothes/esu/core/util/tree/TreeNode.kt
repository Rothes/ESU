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

package io.github.rothes.esu.core.util.tree

class TreeNode<T> private constructor(val parent: TreeNode<T>?, val key: String?) {

    constructor(): this(null, null)

    private val map = hashMapOf<String, TreeNode<T>>()

    val path: String = buildString {
        var node: TreeNode<T>? = this@TreeNode
        while (node?.key != null) {
            insert(0, '/')
            insert(0, node.key)
            node = node.parent
        }
    }
    var value: T? = null

    fun getNode(name: String): TreeNode<T>? {
        return map[name]
    }

    fun getNode(path: List<String>): TreeNode<T>? {
        var node = this
        for (name in path) {
            node = node.getNode(name) ?: return null
        }
        return node
    }

    fun getOrCreateNode(name: String): TreeNode<T> {
        return map.getOrPut(name) { TreeNode(this, name) }
    }

    fun getOrCreateNode(name: String, value: T): TreeNode<T> {
        return getOrCreateNode(name).also { it.value = value }
    }

    fun getOrCreateNode(path: List<String>): TreeNode<T> {
        var node = this
        for (name in path) {
            node = node.getOrCreateNode(name)
        }
        return node
    }

    override fun toString(): String {
        return "TreeNode(key=$key, map=$map, value=$value)"
    }

}