repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
}

val sourcesRelocate: (Project, List<String>, (String) -> String) -> Unit by rootProject.extra

sourcesRelocate(project, listOf("net.kyori")) { it }