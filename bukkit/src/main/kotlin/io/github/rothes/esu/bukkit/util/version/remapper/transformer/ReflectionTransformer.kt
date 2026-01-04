package io.github.rothes.esu.bukkit.util.version.remapper.transformer

import net.neoforged.srgutils.IMappingFile
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

class ReflectionTransformer(
    mapping: IMappingFile,
): ClassNodeTransformer(mapping) {

    override fun transform(classNode: ClassNode): Boolean {
        var modified = false
        for (method in classNode.methods) {
            val instructions = method.instructions.toArray()
            modified = modified or transformDeclaredField(instructions) or transformDeclaredMethod(instructions)
        }
        return modified
    }

    private fun transformDeclaredField(instructions: Array<AbstractInsnNode>): Boolean {
        var modified = false
        for (i in 0 until instructions.size - 3) {
            if ((instructions[i].isOp(Opcodes.INVOKEVIRTUAL) || instructions[i].isOp(Opcodes.LDC))
                && instructions[i + 1].isOp(Opcodes.LDC)
                && instructions[i + 2].isOp(Opcodes.INVOKEVIRTUAL)) {

                val call = instructions[i + 2] as MethodInsnNode
                if (call.owner != "java/lang/Class"
                    || call.name != "getDeclaredField"
                    || call.desc != "(Ljava/lang/String;)Ljava/lang/reflect/Field;"
                ) continue

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

    private fun transformDeclaredMethod(instructions: Array<AbstractInsnNode>): Boolean {
        var modified = false
        var i = instructions.size
        while (--i >= 3) {
            if (instructions[i].isOp(Opcodes.INVOKEVIRTUAL)) {
                val call = instructions[i] as MethodInsnNode
                if (call.owner != "java/lang/Class"
                    || call.name != "getDeclaredMethod"
                    || call.desc != "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"
                ) continue

                val params = mutableListOf<String>()
                var ldc = i - 1
                var found = false
                while (ldc > 0) {
                    if (instructions[ldc].isOp(Opcodes.LDC) && instructions[ldc - 1].isOp(Opcodes.LDC)) {
                        found = true
                        break
                    } else if (instructions[ldc].isOp(Opcodes.GETSTATIC)) {
                        val node = instructions[ldc] as FieldInsnNode
                        if (node.name == "TYPE") {
                            when(node.owner) {
                                "java/lang/Boolean" -> params.add("Z")
                                "java/lang/Byte" -> params.add("B")
                                "java/lang/Character" -> params.add("C")
                                "java/lang/Short" -> params.add("S")
                                "java/lang/Integer" -> params.add("I")
                                "java/lang/Long" -> params.add("J")
                                "java/lang/Float" -> params.add("F")
                                "java/lang/Double" -> params.add("D")
                            }
                        }
                        // TODO class param?
                    }
                    ldc--
                }
                if (!found) continue // Cannot find class and method name
                val clazzNode = instructions[ldc - 1] as LdcInsnNode
                val methodNode = instructions[ldc] as LdcInsnNode

                val clazz = (clazzNode.cst as Type).internalName
                val method = methodNode.cst as String

                params.reverse()
                val paramsString = params.joinToString("")
                val iClass = mapping.getClass(clazz) ?: continue
                val mapped = iClass.methods.find {
                    it.original == method && it.descriptor.substringBefore(')').substring(1) == paramsString
                } ?: continue
                methodNode.cst = mapped.mapped
                modified = true
            }
        }
        return modified
    }

    private fun AbstractInsnNode.isOp(opcode: Int): Boolean {
        return this.opcode == opcode
    }

}