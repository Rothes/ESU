repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Rothes.Configurate:configurate-yaml:master-SNAPSHOT")
}

val sourcesRelocate: (Project, List<String>, (String) -> String) -> Unit by rootProject.extra

sourcesRelocate(project, listOf("org.spongepowered", "net.kyori")) {
    val destPrefix = "io.github.rothes.${rootProject.name.lowercase()}.lib."
    it.replace("org.yaml.snakeyaml", destPrefix + "org.spongepowered.configurate.yaml.internal.snakeyaml")
}