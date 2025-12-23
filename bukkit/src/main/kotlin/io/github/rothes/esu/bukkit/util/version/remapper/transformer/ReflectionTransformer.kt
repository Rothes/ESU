package io.github.rothes.esu.bukkit.util.version.remapper.transformer

import net.neoforged.art.api.Transformer
import net.neoforged.srgutils.IMappingFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class ReflectionTransformer(
    val mapping: IMappingFile,
): Transformer {

    override fun process(entry: Transformer.ClassEntry): Transformer.ClassEntry {
        val reader = ClassReader(entry.data)
        val classNode = ClassNode()
        reader.accept(classNode, 0)
        var modified = false
        for (method in classNode.methods) {
            modified = modified || handleMethod(method)
        }
        if (!modified)
            return entry

        val writer = ClassWriter(0)
        classNode.accept(writer)

        val data = writer.toByteArray()
        return if (entry.isMultiRelease)
            Transformer.ClassEntry.create(entry.className, entry.time, data, entry.version)
        else
            Transformer.ClassEntry.create(entry.name, entry.time, data)
    }

    private fun handleMethod(method: MethodNode): Boolean {
        var modified = false
        val instructions = method.instructions.toArray()
        for (i in 0 until instructions.size - 3) {
            if ((instructions[i].opcode and (Opcodes.INVOKEVIRTUAL or Opcodes.LDC) != 0)
                && instructions[i + 1].opcode == Opcodes.LDC &&
                instructions[i + 2].opcode == Opcodes.INVOKEVIRTUAL) {

                val call = instructions[i + 2] as MethodInsnNode
                if (call.owner != "java/lang/Class" || call.name != "getDeclaredField"
                    || call.desc != "(Ljava/lang/String;)Ljava/lang/reflect/Field;")
                    continue

                val fieldNode = instructions[i + 1] as LdcInsnNode
                if (fieldNode.cst !is String) continue
                val field = fieldNode.cst as String

                val classInfo = instructions[i]
                val clazz = if (classInfo is LdcInsnNode) {
                    (classInfo.cst as Type).internalName
                } else {
                    classInfo as MethodInsnNode
                    if (classInfo.name != "getClass" || classInfo.desc != "()Ljava/lang/Class;") continue
                    classInfo.owner
                }

                val iClass = mapping.getClass(clazz) ?: continue
                val mapped = iClass.getField(field) ?: continue
                fieldNode.cst = mapped.mapped
                modified = true
            }
        }
        return modified
    }

}