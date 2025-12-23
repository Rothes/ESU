package io.github.rothes.esu.bukkit.util.version.remapper.transformer

import net.neoforged.art.api.Transformer
import net.neoforged.srgutils.IMappingFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class KotlinMetaTransformer(
    val mapping: IMappingFile,
): Transformer {

    override fun process(entry: Transformer.ClassEntry): Transformer.ClassEntry {
        val reader = ClassReader(entry.data)
        val classNode = ClassNode()
        reader.accept(classNode, 0)
        val modified = checkAnnotations(classNode.visibleAnnotations)

        if (!modified) return entry

        val writer = ClassWriter(0)
        classNode.accept(writer)

        val data = writer.toByteArray()
        return if (entry.isMultiRelease)
            Transformer.ClassEntry.create(entry.className, entry.time, data, entry.version)
        else
            Transformer.ClassEntry.create(entry.name, entry.time, data)
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
                    if (!str.startsWith('L')) continue
                    val clazz = mapping.getClass(str.substring(1, str.length - 1)) ?: continue
                    iterator1.set("L${clazz.mapped};")
                    modified = true
                }
            }
        }
        return modified
    }

}