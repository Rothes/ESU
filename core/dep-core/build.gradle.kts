plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    implementation(libs.adventure.api)
    implementation(libs.adventure.text.minimessage)
    implementation(libs.adventure.text.serializer.ansi)
    implementation(libs.adventure.text.serializer.gson) {
        exclude("com.google.code.gson")
    }
    implementation(libs.adventure.text.serializer.legacy)
    implementation(libs.adventure.text.serializer.plain)

    implementation(libs.packetevents.api)
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
    relocate("com.github.retrooper.packetevents", "packetevents")
    relocate("io.github.retrooper.packetevents", "packetevents")
}