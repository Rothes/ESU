plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()
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
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
}