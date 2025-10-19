repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    api(kotlin("reflect"))

    compileOnlyApi(project(":core:dep-core", configuration = "shadow"))
    api(project(":core:dep-impl-core", configuration = "shadow"))

    compileOnlyApi(libs.exposed.core)
    compileOnlyApi(libs.exposed.jdbc)
    compileOnlyApi(libs.exposed.kotlin.datetime)
    compileOnlyApi(libs.exposed.json)
    compileOnlyApi(libs.hikariCP)

    compileOnlyApi("org.incendo:cloud-core:2.0.0")
    compileOnlyApi("org.incendo:cloud-annotations:2.0.0")
    compileOnlyApi("org.incendo:cloud-kotlin-coroutines-annotations:2.0.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }

    val adventureVersion = rootProject.libs.versions.adventure.get()
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-gson:$adventureVersion") {
        exclude("com.google.code.gson")
    }

    compileOnly("com.google.code.gson:gson:2.11.0")

    compileOnly("org.apache.maven:maven-resolver-provider:3.9.6")
    compileOnly("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.18")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.9.18")

    compileOnly("org.ow2.asm:asm:9.8")
    compileOnly("org.ow2.asm:asm-commons:9.8")

    compileOnly("com.google.guava:guava:33.3.1-jre")
    compileOnly("it.unimi.dsi:fastutil:8.5.15")
}

tasks.test {
    useJUnitPlatform()
}