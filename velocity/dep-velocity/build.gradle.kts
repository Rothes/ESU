plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
}