plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow")
}

interface SourcesFatJarExtension {
    val relocates: ListProperty<String>
    val postSources: Property<(String) -> String>
}

val extension = project.extensions.create<SourcesFatJarExtension>("sourcesFatJar")
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
    val replace = { str: String ->
        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
        fun String.relocate(s: String) = replace(s, destPrefix + s)
        var res = str
        extension.relocates.get().forEach { res = res.relocate(it) }
        extension.postSources.get().invoke(res)
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
                        into(
                            "io/github/rothes/${rootProject.name.lowercase()}/lib/" + path.substringBeforeLast(
                                "/"
                            )
                        )
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
        val pkg = "io.github.rothes.${rootProject.name.lowercase()}.lib"
        fun relocate(pattern: String) {
            relocate(pattern, "$pkg.$pattern")
        }

        extension.relocates.get().forEach { relocate(it) }

        mergeServiceFiles()
    }

    dependsOn(sourcesFatJar)
}