plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    implementation(libs.packetevents.velocity) {
        exclude(group = "com.github.retrooper", module = "packetevents-api")
    }
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
    relocate("com.github.retrooper.packetevents", "packetevents")
    relocate("io.github.retrooper.packetevents", "packetevents")
}