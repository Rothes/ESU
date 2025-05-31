repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

val exposedVersion: String by project

dependencies {
    api(kotlin("reflect"))

    api("com.zaxxer:HikariCP:6.3.0")
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
//    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    api("org.jetbrains.exposed:exposed-json:${exposedVersion}")

    api("org.incendo:cloud-core:2.0.0")
    api("org.incendo:cloud-annotations:2.0.0")
    api("org.incendo:cloud-kotlin-coroutines-annotations:2.0.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
    api("org.spongepowered:configurate-yaml:4.2.0-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
//    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks.test {
    useJUnitPlatform()
}