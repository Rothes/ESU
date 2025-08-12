repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    val adventureVersion: String by project
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-ansi:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion") {
        exclude("com.google.code.gson")
    }
    implementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    implementation("com.github.Rothes.Configurate:configurate-yaml:master-SNAPSHOT")
}

val sourcesRelocate: (Project, List<String>, (String) -> String) -> Unit by rootProject.extra

sourcesRelocate(project, listOf("org.spongepowered", "net.kyori")) {
        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
    it.replace("org.yaml.snakeyaml", destPrefix + "org.spongepowered.configurate.yaml.internal.snakeyaml")
}

//
//tasks.shadowJar {
//    val pkg = "io.github.rothes.${rootProject.name.lowercase()}.lib"
//    fun relocate(pattern: String) {
//        relocate(pattern, "$pkg.$pattern")
//    }
//    relocate("org.spongepowered")
//    relocate("net.kyori.option")
//
//    dependencies {
//        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
//        exclude(dependency("org.jetbrains:annotations"))
//    }
//
//    mergeServiceFiles()
//
//    dependsOn(sourcesFatJar)
//}
//
//val sourcesFatJar = tasks.register("sourcesFatJar", Jar::class) {
//    dependsOn(tasks.classes)
//    group = "build"
//    archiveClassifier.value("sources")
//
//    val tmpDir = temporaryDir
//    tmpDir.deleteRecursively()
//
//    val result = dependencies.createArtifactResolutionQuery()
//        .forComponents(
//            configurations.runtimeClasspath.get().incoming.resolutionResult
//                .allDependencies
//                .filter {
//                    !it.from.id.displayName.startsWith("org.jetbrains.kotlin:kotlin-stdlib")
//                }
//                .map { it.from.id }
//        )
//        .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java)
//        .execute()
//    val replace = { str: String ->
//        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
//        fun String.relocate(s: String) = replace(s, destPrefix + s)
//        str.relocate("org.spongepowered").relocate("net.kyori.option")
//            .replace("org.yaml.snakeyaml", destPrefix + "org.spongepowered.configurate.yaml.internal.snakeyaml")
//    }
//    for (component in result.resolvedComponents) {
//        component.getArtifacts(SourcesArtifact::class.java).forEach {
//            if (it is ResolvedArtifactResult) {
//                zipTree(it.file.absolutePath).visit {
//                    if (path.startsWith("META-INF") || file.isDirectory)
//                        return@visit
//                    val tmp = tmpDir.resolve(path)
//                    tmp.parentFile.mkdirs()
//                    tmp.writeText(replace(file.readText()))
//                    from(tmp) {
//                        duplicatesStrategy = DuplicatesStrategy.WARN
//                        into("io/github/rothes/${rootProject.name.lowercase()}/lib/" + path.substringBeforeLast("/"))
//                    }
//                }
//            }
//        }
//    }
//}
//
//publishing {
//    repositories {
//        mavenLocal()
//    }
//    publications {
//        create<MavenPublication>("mavenJar") {
//            from(components["shadow"])
//
//            artifact(sourcesFatJar) {
//                classifier = "sources"
//            }
//
//            artifactId = project.name
//            groupId = project.group as String?
//            version = project.version as String?
//        }
//    }
//}