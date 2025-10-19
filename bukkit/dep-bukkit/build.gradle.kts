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
    implementation("de.tr7zw:item-nbt-api:2.15.3")
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")

    relocate("de.tr7zw.changeme.nbtapi", "nbtapi")
    relocate("org.bstats", "bstats")
}