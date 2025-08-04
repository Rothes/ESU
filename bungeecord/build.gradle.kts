import org.apache.tools.ant.filters.ReplaceTokens

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://libraries.minecraft.net")
    maven("https://repo.codemc.org/repository/maven-public/")

}


dependencies {
    compileOnly("net.md-5:bungeecord-api:1.21-R0.2")

    api(project(":core"))
    api("org.incendo:cloud-bungee:2.0.0-beta.10")

    implementation("com.h2database:h2:2.3.232")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3")

    implementation("net.kyori:adventure-platform-bungeecord:4.4.0")
    implementation("net.kyori:adventure-text-minimessage:4.21.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.21.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.21.0")

//    implementation("org.bstats:bstats-bukkit:3.1.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-java-parameters") // Fix cloud-annotations
    }
}

val fileName = "${rootProject.name}-${project.name}"
tasks.shadowJar {
    archiveFileName = "${fileName}-${project.version}.jar"

    val pkg = "io.github.rothes.${rootProject.name.lowercase()}.lib"
    relocate("org.spongepowered", "$pkg.org.spongepowered")
    relocate("net.kyori", "$pkg.net.kyori")

    relocate("org.bstats", "$pkg.org.bstats")

    mergeServiceFiles()
}

tasks.processResources {
    val keys = listOf("versionName", "versionChannel", "versionId")
    val properties = rootProject.ext.properties.filter { keys.contains(it.key) }
    inputs.properties(properties)
    filter<ReplaceTokens>("tokens" to properties)
}

buildConfig {
    buildConfigField("PLUGIN_PLATFORM", "bungeecord")
}