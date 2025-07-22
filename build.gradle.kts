import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    kotlin("jvm") version "2.1.21"
    `maven-publish`
    id("com.gradleup.shadow") version "9.0.0-rc1"
    id("com.xpdustry.kotlin-shadow-relocator") version "3.0.0-rc.1"
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
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "com.xpdustry.kotlin-shadow-relocator")

    val javaVer = JavaVersion.VERSION_17

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
    }
}