package io.github.rothes.esu.bukkit.util.version.remapper.transformer

import net.neoforged.art.api.Transformer
import net.neoforged.srgutils.IMappingFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

abstract class ClassNodeTransformer(
    val mapping: IMappingFile,
): Transformer {

    abstract fun transform(classNode: ClassNode): Boolean

    override fun process(entry: Transformer.ClassEntry): Transformer.ClassEntry {
        val reader = ClassReader(entry.data)
        val classNode = ClassNode()
        reader.accept(classNode, 0)

        if (!transform(classNode)) return entry

        val writer = ClassWriter(0)
        classNode.accept(writer)

        val data = writer.toByteArray()
        return if (entry.isMultiRelease)
            Transformer.ClassEntry.create(entry.className, entry.time, data, entry.version)
        else
            Transformer.ClassEntry.create(entry.name, entry.time, data)
    }

}