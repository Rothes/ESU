plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    implementation("io.github.rothes:configurate-yaml:4.3.0-SNAPSHOT")
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
    relocate("net.kyori.option", "koption")
    relocate("org.spongepowered.configurate", "configurate")

    postSources.set {
        val identifier = it.artifact.id.componentIdentifier
        if (identifier !is ModuleComponentIdentifier || !identifier.moduleIdentifier.name.startsWith("configurate"))
            return@set it.content

        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
        it.content.replace("org.yaml.snakeyaml", destPrefix + "configurate.yaml.internal.snakeyaml")
    }
}