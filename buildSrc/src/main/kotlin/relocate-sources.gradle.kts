plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow")
}

interface RelocateSourcesExtension {
    val relocates: ListProperty<Relocate>
    val postSources: Property<(String) -> String>

    data class Relocate(
        val original: String,
        val relocated: String = original,
    )
}

val extension = project.extensions.create<RelocateSourcesExtension>("relocateSources")
extension.postSources.convention { it }

val sourcesFatJar = tasks.register("sourcesFatJar", Jar::class) {
    dependsOn(tasks.classes)
    group = "build"
    archiveClassifier.value("sources")

    val tmpDir = temporaryDir
    tmpDir.deleteRecursively()

    val result = dependencies.createArtifactResolutionQuery()
        .forComponents(configurations.runtimeClasspath.get().incoming.resolutionResult.allDependencies.filter {
            !it.from.id.displayName.startsWith("org.jetbrains.kotlin:kotlin-stdlib")
        }.map { it.from.id }).withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java).execute()

    val relocates = extension.relocates.get().sortedByDescending { it.original.length }
    val replace = { str: String ->
        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
        var res = str
        relocates.forEach { res = res.replace(it.original, destPrefix + it.relocated) }
        extension.postSources.get().invoke(res)
    }
    val rep = relocates.map { relocate ->
        fun String.toPath() = replace('.', '/')
        relocate.original.toPath() to "io/github/rothes/${rootProject.name.lowercase()}/lib/" + relocate.relocated.toPath()
    }
    for (component in result.resolvedComponents) {
        component.getArtifacts(SourcesArtifact::class.java).forEach {
            if (it is ResolvedArtifactResult) {
                zipTree(it.file.absolutePath).visit {
                    if (path.startsWith("META-INF") || file.isDirectory) return@visit
                    val tmp = tmpDir.resolve(path)
                    tmp.parentFile.mkdirs()
                    tmp.writeText(replace(file.readText()))
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