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
                buildConfigField("VERSION_NAME", project.property("versionName").toString())
                buildConfigField("VERSION_CHANNEL", project.property("versionChannel").toString())
                buildConfigField("VERSION_ID", project.property("versionId").toString())
            }
        }
    }
}