import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("java-library")
    kotlin("jvm")
    `no-build-dir`
    `maven-publish`
    id("com.gradleup.shadow")
    id("com.github.gmazzo.buildconfig") version "6.0.6"
}

allprojects {
    group = "io.github.rothes"
    project.version = rootProject.property("versionName").toString()

    repositories {
        mavenLocal()
        mavenCentral()
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