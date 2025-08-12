plugins {
    kotlin("jvm")
}

group = "io.github.rothes.esu"
version = "0.8.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}