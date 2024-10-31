plugins {
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "io.github.rothes.esu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    api("cc.carm.lib:easysql-hikaricp:0.4.7")
    api("org.incendo:cloud-core:2.0.0")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks.test {
    useJUnitPlatform()
}