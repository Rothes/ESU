plugins {
    `relocate-sources`
    `esu-publishing`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    implementation(libs.nbt.api)
    implementation(libs.bstats.bukkit)
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")

    relocate("de.tr7zw.changeme.nbtapi", "nbtapi")
    relocate("org.bstats", "bstats")
}

esuPublishing {
    artifactIdOverride = "esu-bukkit-lib"
    useShadow = true
}