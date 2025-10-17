plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
}

relocateSources {
    relocates.add(Relocate_sources_gradle.RelocateSourcesExtension.Relocate("net.kyori"))
    relocates.add(Relocate_sources_gradle.RelocateSourcesExtension.Relocate("net.kyori.adventure", "adventure"))
}