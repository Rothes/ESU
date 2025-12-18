plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Rothes.Configurate:configurate-yaml:master-SNAPSHOT")
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
    relocate("net.kyori.option", "koption")
    relocate("org.spongepowered.configurate", "configurate")

    postSources.set {
        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
        it.replace("org.yaml.snakeyaml", destPrefix + "configurate.yaml.internal.snakeyaml")
    }
}