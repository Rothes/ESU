import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("java-library")
    kotlin("jvm")
    `maven-publish`
    id("com.gradleup.shadow")
    id("com.github.gmazzo.buildconfig") version "6.0.6"
}

allprojects {
    group = "io.github.rothes.esu"
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

        apply(plugin = "com.github.gmazzo.buildconfig")
        buildConfig {
            when (project.name) {
                "common" -> {}
                "core" -> {
                    buildConfigField("DEP_VERSION_KOTLIN", rootProject.libs.versions.kotlin)
                    buildConfigField("DEP_VERSION_KOTLINX_IO_CORE", rootProject.libs.versions.kotlinx.io.core)
                }
                else -> {
                    apply(plugin = "publish-modrinth")
                    buildConfigField("VERSION_NAME", provider { finalVersionName })
                    buildConfigField("VERSION_CHANNEL", project.property("versionChannel").toString())
                    buildConfigField("VERSION_ID", project.property("versionId").toString())
                    buildConfigField("DEP_VERSION_ADVENTURE", rootProject.libs.versions.adventure)
                    buildConfigField("DEP_VERSION_EXPOSED", rootProject.libs.versions.exposed)
                    buildConfigField("DEP_VERSION_H2DATABASE", rootProject.libs.versions.h2database)
                    buildConfigField("DEP_VERSION_HIKARICP", rootProject.libs.versions.hikariCP)
                    buildConfigField("DEP_VERSION_MARIADB_CLIENT", rootProject.libs.versions.mariadb.client)
                    buildConfigField("DEP_VERSION_PACKETEVENTS", rootProject.libs.versions.packetevents)
                }
            }
        }
    }
}