/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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