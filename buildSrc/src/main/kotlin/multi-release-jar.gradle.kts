import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
}

interface MultiReleaseJarExtension {
    val javaVersions: ListProperty<JavaVersion>
}

val extension = project.extensions.create<MultiReleaseJarExtension>("multiReleaseJar")

project.afterEvaluate {
    val javaVersions = extension.javaVersions.get().sortedBy { it.ordinal }

    val mainSourceSet = project.sourceSets.getByName("main")

    for (javaVer in javaVersions) {
        val number = javaVer.majorVersion

        sourceSets {
            create("java$number") {
                kotlin {
                    srcDir("src/main/kotlin-jvm$number")
                }
                compileClasspath += mainSourceSet.compileClasspath
                compileClasspath += mainSourceSet.output
            }
        }

        tasks.named<KotlinCompile>("compileJava${number}Kotlin") {
            compilerOptions.jvmTarget = JvmTarget.fromTarget(number)
        }
        tasks.named<JavaCompile>("compileJava${number}Java") {
            sourceCompatibility = number
            targetCompatibility = number
        }

        tasks.named("classes") {
            finalizedBy(tasks.named("java${number}Classes"))
        }

        tasks.jar {
            into("META-INF/versions/$number") {
                from(sourceSets["java$number"].output)
            }
            manifest {
                attributes("Multi-Release" to true)
            }
        }
    }
}