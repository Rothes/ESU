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
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")

    relocate("de.tr7zw.changeme.nbtapi", "nbtapi")
    relocate("org.bstats", "bstats")
}