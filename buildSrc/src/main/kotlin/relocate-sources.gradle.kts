plugins {
    java
    `maven-publish`
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

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJar") {
            from(components["shadow"])

            artifact(sourcesFatJar) {
                classifier = "sources"
            }

            artifactId = project.name
            groupId = project.group as String?
            version = project.version as String?
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