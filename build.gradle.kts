import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    kotlin("jvm") version "2.1.10"
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6"
    id("com.xpdustry.kotlin-shadow-relocator") version "2.0.0"
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

subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "com.xpdustry.kotlin-shadow-relocator")

    group = "io.github.rothes.esu"
    version = rootProject.property("versionName").toString()

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