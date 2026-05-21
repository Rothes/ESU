plugins {
    `relocate-sources`
    `esu-publishing`
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

esuPublishing {
    artifactIdOverride = "esu-velocity-lib"
    useShadow = true
}
