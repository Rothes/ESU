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
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
}