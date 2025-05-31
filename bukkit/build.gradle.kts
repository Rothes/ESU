import com.xpdustry.ksr.kotlinRelocate
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("io.papermc.paperweight.userdev") version "1.7.5"
}

val serverVer = rootProject.property("targetMinecraftVersion").toString()

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "PlugManX"
        url = uri("https://raw.githubusercontent.com/TheBlackEntity/PlugManX/repository/")
    }

    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public/")

    maven("https://mvn.lumine.io/repository/maven-public")
    maven {
        name = "MMOItems"
        url = uri("https://nexus.phoenixdevt.fr/repository/maven-public/")
    }
    maven("https://repo.momirealms.net/releases/")
}


dependencies {
    compileOnly(project(":bukkit:compat:bukkit_1_12_5", configuration = "shadow"))
    paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
    api(project(":core"))
    api("org.incendo:cloud-paper:2.0.0-beta.10")

    implementation("com.h2database:h2:2.3.232")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3")

    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")

    compileOnly("com.rylinaux:PlugManX:2.4.1")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")

    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1")
    compileOnly("io.lumine:Mythic-Dist:5.8.0")
    compileOnly("io.lumine:MythicLib-dist:1.7.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.9.5-SNAPSHOT")

    compileOnly("fr.xephi:authme:5.6.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("org.bukkit")
    }
    compileOnly("net.momirealms:craft-engine-core:0.0.49")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.49")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-java-parameters") // Fix cloud-annotations
    }
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

val fileName = "${rootProject.name}-${project.name}"
tasks.shadowJar {
    archiveFileName = "${fileName}-${project.version}-mojmap.jar"

    kotlinRelocate("kotlin.", "io.github.rothes.esu.lib.kotlin.") {
        exclude("%regex[.+\\.kotlin_builtins]") // Fix issues with kotlin-reflect
    }
    kotlinRelocate("kotlinx.", "io.github.rothes.esu.lib.kotlinx.")
    kotlinRelocate("org.jetbrains.exposed.", "io.github.rothes.esu.lib.org.jetbrains.exposed.")
    kotlinRelocate("org.incendo", "io.github.rothes.esu.lib.org.incendo")
    relocate("com.zaxxer", "io.github.rothes.esu.lib.com.zaxxer")
    relocate("org.spongepowered", "io.github.rothes.esu.lib.org.spongepowered")

    relocate("info.debatty", "io.github.rothes.esu.lib.info.debatty")
    relocate("org.bstats", "io.github.rothes.esu.lib.org.bstats")

    mergeServiceFiles()
}

tasks.processResources {
    val keys = listOf("versionName", "versionChannel", "versionId")
    val properties = rootProject.ext.properties.filter { keys.contains(it.key) }
    inputs.properties(properties)
    filter<ReplaceTokens>("tokens" to properties)
}
