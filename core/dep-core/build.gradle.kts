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
    relocates.add(Relocate_sources_gradle.RelocateSourcesExtension.Relocate("net.kyori"))
    relocates.add(Relocate_sources_gradle.RelocateSourcesExtension.Relocate("net.kyori.adventure", "adventure"))
}