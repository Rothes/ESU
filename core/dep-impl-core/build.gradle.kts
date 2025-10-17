plugins {
    `sources-fat-jar`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Rothes.Configurate:configurate-yaml:master-SNAPSHOT")
}

sourcesFatJar {
    relocates.add("net.kyori")
    relocates.add("org.spongepowered")

    postSources.set {
        val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
        it.replace("org.yaml.snakeyaml", destPrefix + "org.spongepowered.configurate.yaml.internal.snakeyaml")
    }
}