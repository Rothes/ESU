package io.github.rothes.esu.core.util.artifact

import io.github.rothes.esu.core.EsuCore
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class PackageRelocator(
    relocates: Map<String, String>,
    val logger: (String) -> Unit = { EsuCore.instance.info("[Relocator] $it") }
) {

    private val remapper = ClassNameRemapper(relocates)

    fun relocate(input: File, output: File) {
        val start = System.currentTimeMillis()
        logger("Relocating $input to $output")
        val entries = buildList {
            ZipFile(input).use { file ->
                for (e in file.entries().iterator()) {
                    if (e.isDirectory) continue
                    val name = e.name
                    val data = file.getInputStream(e).use { it.readAllBytes() }
                    if (name.endsWith(".class")) {
                        add(ClassEntry(name, e.time, data))
                    } else {
                        add(Entry(name, e.time, data))
                    }
                }
            }
        }
        val mapped = entries.map { entry ->
            if (entry is ClassEntry) {
                val reader = ClassReader(entry.data)
                val writer = ClassWriter(0)
                val visitor = RelocateVisitor(writer, remapper)

                reader.accept(visitor, 0)

                val data = writer.toByteArray()
                val newName = remapper.map(entry.className)
                ClassEntry("$newName.class", entry.time, data)
            } else {
                entry
            }
        }.sortedBy { it.name }

        val added = mutableSetOf<String>()
        val time = System.currentTimeMillis()
        output.outputStream().buffered().use { bs ->
            ZipOutputStream(bs).use { zip ->
                for (e in mapped) {
                    fun putDirectory(path: String) {
                        if (!added.add(path))
                            return

                        val dir = path.lastIndexOf('/')
                        if (dir > 0)
                            putDirectory(path.take(dir))

                        val entry = ZipEntry("$path/")
                        entry.time = time
                        zip.putNextEntry(entry)
                        zip.closeEntry()
                    }
                    val dir = e.name.lastIndexOf('/')
                    if (dir > 0)
                        putDirectory(e.name.take(dir))

                    val entry = ZipEntry(e.name)
                    entry.time = e.time
                    zip.putNextEntry(entry)
                    zip.write(e.data)
                    zip.closeEntry()
                }
            }
        }
        logger("Relocate done in ${System.currentTimeMillis() - start}ms")
    }

    private open class Entry(
        val name: String,
        val time: Long,
        val data: ByteArray,
    )

    private class ClassEntry(
        name: String,
        time: Long,
        data: ByteArray,
    ): Entry(name, time, data) {
        val className = name.dropLast(6)
    }

    private class ClassNameRemapper(private val relocates: Map<String, String>): Remapper() {

        override fun map(internalName: String): String {
            var mapped = internalName
            for ((from, to) in relocates) {
                if (mapped.startsWith(from))
                    mapped = to + mapped.substring(from.length)
            }
            return mapped
        }

    }

    private class RelocateVisitor(
        writer: ClassWriter,
        remapper: ClassNameRemapper
    ): ClassRemapper(writer, remapper)

}