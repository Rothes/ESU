repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

val exposedVersion: String by project

dependencies {
    api(kotlin("reflect"))

    api(project(":core:configurate", configuration = "shadow"))
    compileOnlyApi("org.jetbrains.exposed:exposed-core:$exposedVersion")
//    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    compileOnlyApi("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    compileOnlyApi("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    compileOnlyApi("org.jetbrains.exposed:exposed-json:$exposedVersion")
    compileOnlyApi("com.zaxxer:HikariCP:6.3.0")

    compileOnlyApi("org.incendo:cloud-core:2.0.0")
    compileOnlyApi("org.incendo:cloud-annotations:2.0.0")
    compileOnlyApi("org.incendo:cloud-kotlin-coroutines-annotations:2.0.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }

    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
//    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
    compileOnly("com.google.code.gson:gson:2.11.0")

    compileOnly("org.apache.maven:maven-resolver-provider:3.9.6")
    compileOnly("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.18")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.9.18")
}

tasks.test {
    useJUnitPlatform()
}