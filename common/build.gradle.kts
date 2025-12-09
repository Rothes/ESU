repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    api(project(":module:off-heap-memory"))
    compileOnly(libs.packetevents.api)
}