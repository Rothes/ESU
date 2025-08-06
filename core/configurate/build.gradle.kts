repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Rothes.Configurate:configurate-yaml:master-SNAPSHOT")
}

tasks.shadowJar {
    val pkg = "io.github.rothes.${rootProject.name.lowercase()}.lib"
    fun relocate(pattern: String) {
        relocate(pattern, "$pkg.$pattern")
    }
    relocate("org.spongepowered")
    relocate("net.kyori.option")

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        exclude(dependency("org.jetbrains:annotations"))
    }

    mergeServiceFiles()
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJar") {
            from(components["shadow"])

            artifactId = project.name
            groupId = project.group as String?
            version = project.version as String?
        }
    }
}