repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    compileOnly(libs.packetevents.api)
}