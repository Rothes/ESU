import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("java-library")
    kotlin("jvm") version "2.2.0"
    `maven-publish`
    id("com.gradleup.shadow") version "9.0.0-rc1"
//    id("com.xpdustry.kotlin-shadow-relocator") version "3.0.0-rc.1"
    id("com.github.gmazzo.buildconfig") version "5.5.1"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

allprojects {
    group = "io.github.rothes.esu"
    project.version = rootProject.property("versionName").toString()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")

    val javaVer = JavaVersion.VERSION_11

    java {
        disableAutoTargetJvm()
        sourceCompatibility = javaVer
        targetCompatibility = javaVer
        withSourcesJar()
        withJavadocJar()
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVer.toString()
        targetCompatibility = javaVer.toString()
    }

    kotlin {
        compilerOptions {
            jvmTarget.value(JvmTarget.fromTarget(javaVer.toString()))
        }
    }

    tasks.javadoc {
        options.encoding = "UTF-8"
    }

    if (project.parent == rootProject) {
        publishing {
            repositories {
                mavenLocal()
            }
            publications {
                create<MavenPublication>("mavenJar") {
                    from(components["java"])

                    artifactId = project.name
                    groupId = project.group as String?
                    version = project.version as String?
                }
            }
        }

        if (project.name != "core") {
            apply(plugin = "com.github.gmazzo.buildconfig")
            buildConfig {
                val exposedVersion: String by project
                val adventureVersion: String by project
                buildConfigField("VERSION_NAME", project.property("versionName").toString())
                buildConfigField("VERSION_CHANNEL", project.property("versionChannel").toString())
                buildConfigField("VERSION_ID", project.property("versionId").toString())
                buildConfigField("EXPOSED_VERSION", exposedVersion)
                buildConfigField("ADVENTURE_VERSION", adventureVersion)
            }
        }
    }
}

val sourcesRelocate by extra {
    fun(project: Project, relocates: List<String>, postSources: (String) -> String) {
        with(project) {
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
                    relocates.forEach { res = res.relocate(it) }
                    postSources(res)
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

            tasks.shadowJar {
                val pkg = "io.github.rothes.${rootProject.name.lowercase()}.lib"
                fun relocate(pattern: String) {
                    relocate(pattern, "$pkg.$pattern")
                }
                relocates.forEach { relocate(it) }

                dependencies {
                    exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
                    exclude(dependency("org.jetbrains:annotations"))
                }

                mergeServiceFiles()

                dependsOn(sourcesFatJar)
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
        }
    }
}