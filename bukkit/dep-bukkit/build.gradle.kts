plugins {
    `sources-fat-jar`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
}

sourcesFatJar {
    relocates.add("net.kyori")
}