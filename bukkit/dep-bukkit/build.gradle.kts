plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
    implementation(libs.nbt.api)
    implementation(libs.bstats.bukkit)

    implementation(libs.packetevents.spigot) {
        exclude(group = "com.github.retrooper", module = "packetevents-api")
    }
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
    relocate("com.github.retrooper.packetevents", "packetevents")
    relocate("io.github.retrooper.packetevents", "packetevents")

    relocate("de.tr7zw.changeme.nbtapi", "nbtapi")
    relocate("org.bstats", "bstats")
}