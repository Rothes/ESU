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
    relocates.add(Relocate_sources_gradle.RelocateSourcesExtension.Relocate("net.kyori"))
    relocates.add(Relocate_sources_gradle.RelocateSourcesExtension.Relocate("net.kyori.adventure", "adventure"))
    relocates.add(Relocate_sources_gradle.RelocateSourcesExtension.Relocate("net.kyori.option", "koption"))
    relocates.add(Relocate_sources_gradle.RelocateSourcesExtension.Relocate("org.spongepowered.configurate", "configurate"))

    postSources.set {
        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
        it.replace("org.yaml.snakeyaml", destPrefix + "configurate.yaml.internal.snakeyaml")
    }
}