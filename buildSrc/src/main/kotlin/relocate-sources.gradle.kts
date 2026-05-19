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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    java
    id("com.gradleup.shadow")
}

interface RelocateSourcesExtension {
    val relocates: ListProperty<Relocate>
    val postSources: Property<(SourceData) -> String>

    fun relocate(original: String, relocated: String = original) {
        relocates.add(Relocate(original, relocated))
    }

    data class Relocate(
        val original: String,
        val relocated: String = original,
    )

    data class SourceData(
        val artifact: ResolvedArtifactResult,
        val content: String,
    )
}

val extension = project.extensions.create<RelocateSourcesExtension>("relocateSources")
extension.postSources.convention { it.content }

val sourcesFatJar = tasks.register("sourcesFatJar", Jar::class) {
    dependsOn(tasks.classes)
    group = "build"
    archiveClassifier.value("sources")

    val tmpDir = temporaryDir
    tmpDir.deleteRecursively()

    val result = dependencies.createArtifactResolutionQuery()
        .forComponents(
            configurations.runtimeClasspath.get().resolvedConfiguration.lenientConfiguration.artifacts
                .filter { it.name != "kotlin-stdlib" && it.name != "annotations" }
                .map { it.id.componentIdentifier }
        )
        .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java)
        .execute()

    val relocates = extension.relocates.get().sortedByDescending { it.original.length }
    val rep = relocates.map { relocate ->
        fun String.toPath() = replace('.', '/')
        relocate.original.toPath() to "io/github/rothes/${rootProject.name.lowercase()}/lib/" + relocate.relocated.toPath()
    }
    for (component in result.resolvedComponents) {
        component.getArtifacts(SourcesArtifact::class.java).forEach { artifact ->
            if (artifact is ResolvedArtifactResult) {
                zipTree(artifact.file.absolutePath).visit {
                    if (path.startsWith("META-INF") || file.isDirectory) return@visit
                    val tmp = tmpDir.resolve(path)
                    tmp.parentFile.mkdirs()

                    val rawText = file.readText()
                    val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
                    var res = rawText
                    for (relocate in relocates) {
                        res = res.replace(relocate.original, destPrefix + relocate.relocated)
                    }
                    res = extension.postSources.get().invoke(
                        RelocateSourcesExtension.SourceData(artifact, res)
                    )

                    tmp.writeText(res)
                    from(tmp) {
                        duplicatesStrategy = DuplicatesStrategy.WARN
                        var intoPath = path.substringBeforeLast("/")
                        for ((from, dest) in rep) {
                            if (intoPath.startsWith(from)) {
                                intoPath = dest + intoPath.substring(from.length)
                            }
                        }
                        into(intoPath)
                    }
                }
            }
        }
    }
}

tasks.shadowJar {
    doFirst { // Wait for configuration
        val pkg = "io.github.rothes.${rootProject.name.lowercase()}.lib."

        extension.relocates.get()
            .sortedByDescending { it.original.length }
            .forEach { relocate(it.original, pkg + it.relocated) }

        mergeServiceFiles()
    }

    dependsOn(sourcesFatJar)
}