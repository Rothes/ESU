plugins {
    `relocate-sources`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    implementation(libs.adventure.api)
    implementation(libs.adventure.text.minimessage)
    implementation(libs.adventure.text.serializer.ansi)
    implementation(libs.adventure.text.serializer.gson) {
        exclude("com.google.code.gson")
    }
    implementation(libs.adventure.text.serializer.legacy)
    implementation(libs.adventure.text.serializer.plain)

    implementation(libs.configurate.yaml)
}

relocateSources {
    relocate("net.kyori")
    relocate("net.kyori.adventure", "adventure")
    relocate("org.spongepowered.configurate", "configurate")

    postSources.set {
        val identifier = it.artifact.id.componentIdentifier
        if (identifier !is ModuleComponentIdentifier || !identifier.moduleIdentifier.name.startsWith("configurate"))
            return@set it.content

        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
        it.content.replace("org.yaml.snakeyaml", destPrefix + "configurate.yaml.internal.snakeyaml")
    }
}