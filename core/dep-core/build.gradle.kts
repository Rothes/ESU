plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    val adventureVersion = rootProject.libs.versions.adventure.get()
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-ansi:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion") {
        exclude("com.google.code.gson")
    }
    implementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
}