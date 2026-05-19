import dev.yumi.gradle.licenser.api.rule.HeaderRule
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("java-library")
    kotlin("jvm")
    `no-build-dir`
    `maven-publish`
    id("com.gradleup.shadow")
    id("dev.yumi.gradle.licenser") version "4.0.0"
    id("com.github.gmazzo.buildconfig") version "6.0.6"
}

allprojects {
    apply(plugin = "dev.yumi.gradle.licenser")

    group = "io.github.rothes"
    project.version = rootProject.property("versionName").toString()

    repositories {
        mavenLocal()
        mavenCentral()
    }

    license {
        rule(HeaderRule.parse("LICENSE", """
            This file is part of ESU - https://github.com/Rothes/ESU
            Copyright (C) 2026 Rothes & contributers
    
            ESU is free software: you can redistribute it and/or modify it
            under the terms of the GNU General Public License as published
            by the Free Software Foundation, either version 3 of the License,
            or (at your option) any later version.
            
            ESU is distributed in the hope that it will be useful,
            but WITHOUT ANY WARRANTY; without even the implied warranty of
            MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
            See the GNU General Public License for more details.
            
            You should have received a copy of the GNU General Public License
            along with Foobar. If not, see <https://www.gnu.org/licenses/>.
        """.trimIndent().lines()))

        include("**/*.java")
        include("**/*.kt")
    }
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

    tasks.shadowJar {
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
            exclude(dependency("org.jetbrains:annotations"))
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.value(JvmTarget.fromTarget(javaVer.toString()))
            // Remove Intrinsics checks
            freeCompilerArgs.add("-Xno-call-assertions")
            freeCompilerArgs.add("-Xno-receiver-assertions")
            freeCompilerArgs.add("-Xno-param-assertions")
        }
    }

    tasks.javadoc {
        options.encoding = "UTF-8"
    }

    if (project.parent == rootProject) {
        apply(plugin = "esu-publishing")
        apply(plugin = "com.github.gmazzo.buildconfig")
        buildConfig {
            val packageName = "${project.group}.esu.data"
            when (project.name) {
                "common", "module" -> {}
                "core" -> {
                    forClass(packageName, "KotlinVersion") {
                        buildConfigField("KOTLIN", rootProject.libs.versions.kotlin)
                        buildConfigField("KOTLINX_IO_CORE", rootProject.libs.versions.kotlinx.io.core)
                    }
                }

                else               -> {
                    apply(plugin = "publish-modrinth")

                    forClass(packageName, "BuildInfo") {
                        buildConfigField("VERSION_NAME", provider { finalVersionName })
                        buildConfigField("VERSION_CHANNEL", project.property("versionChannel").toString())
                        buildConfigField("VERSION_ID", project.property("versionId").toString())
                    }
                    forClass(packageName, "DependencyVersion") {
                        buildConfigField("ADVENTURE", rootProject.libs.versions.adventure)
                        buildConfigField("EXPOSED", rootProject.libs.versions.exposed)
                        buildConfigField("H2DATABASE", rootProject.libs.versions.h2database)
                        buildConfigField("HIKARICP", rootProject.libs.versions.hikariCP)
                        buildConfigField("MARIADB_CLIENT", rootProject.libs.versions.mariadb.client)
                        buildConfigField("PACKETEVENTS", rootProject.libs.versions.packetevents)
                    }
                }
            }
        }
    }
}