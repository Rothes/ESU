plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.0")
    implementation("com.modrinth.minotaur:com.modrinth.minotaur.gradle.plugin:2.+")
}