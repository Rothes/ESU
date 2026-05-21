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

package io.github.rothes.esu.bukkit.util.version.remapper.transformer

import net.neoforged.srgutils.IMappingFile
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class KotlinMetaTransformer(
    mapping: IMappingFile,
): ClassNodeTransformer(mapping) {

    override fun transform(classNode: ClassNode): Boolean {
        return checkAnnotations(classNode.visibleAnnotations)
    }

    private fun checkAnnotations(nodes: List<AnnotationNode>?): Boolean {
        nodes ?: return false

        var modified = false
        for (node in nodes) {
            if (node.desc != "Lkotlin/Metadata;") continue

            val iterator = node.values.listIterator()
            for (name in iterator) {
                name as String
                val value = iterator.next()
                if (!name.startsWith('d')) continue
                @Suppress("UNCHECKED_CAST")
                value as MutableList<String>
                val iterator1 = value.listIterator()
                for (str in iterator1) {
                    // TODO we are just modifying all strings, but not verifying if it's actually a class param
                    if (!str.startsWith('L') || !str.endsWith(';')) continue
                    val clazz = mapping.getClass(str.substring(1, str.length - 1)) ?: continue
                    iterator1.set("L${clazz.mapped};")
                    modified = true
                }
            }
        }
        return modified
    }

}