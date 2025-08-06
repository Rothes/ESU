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