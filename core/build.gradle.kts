plugins {
    id("com.gradleup.shadow") version "8.3.4"
}

group = "io.github.rothes.esu"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
//    api(kotlin("reflect"))
    api("cc.carm.lib:easysql-hikaricp:0.4.7")
    api("org.incendo:cloud-core:2.0.0")
    api("org.spongepowered:configurate-yaml:4.2.0-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
//    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks.test {
    useJUnitPlatform()
}