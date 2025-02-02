plugins {
    id("com.gradleup.shadow") version "8.3.4"
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
//    api(kotlin("reflect"))
    api("cc.carm.lib:easysql-hikaricp:0.4.7")
    api("org.incendo:cloud-core:2.0.0")
    api("org.incendo:cloud-annotations:2.0.0")
    api("org.incendo:cloud-kotlin-coroutines-annotations:2.0.0")
    api("org.spongepowered:configurate-yaml:4.2.0-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
//    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks.test {
    useJUnitPlatform()
}