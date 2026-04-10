plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.1")
    implementation("com.modrinth.minotaur:com.modrinth.minotaur.gradle.plugin:2.+")
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:${libs.versions.kotlin.get()}")
}