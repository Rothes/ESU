import com.xpdustry.ksr.kotlinRelocate
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
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
    maven {
        name = "PlaceholderAPI"
        url = uri("https://repo.extendedclip.com/releases/")
    }
    maven("https://repo.codemc.org/repository/maven-public/")

    maven("https://mvn.lumine.io/repository/maven-public")
    maven {
        name = "MMOItems"
        url = uri("https://nexus.phoenixdevt.fr/repository/maven-public/")
    }
    maven("https://repo.momirealms.net/releases/")
}


dependencies {
    paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
    api(project(":core"))
    api("org.incendo:cloud-paper:2.0.0-beta.10")

    implementation("com.h2database:h2:2.3.232")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3")

    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")

    implementation("de.tr7zw:item-nbt-api:2.15.1")

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

    val pkg = "io.github.rothes.${rootProject.name.lowercase()}.${project.name.lowercase()}.lib"
    kotlinRelocate("kotlin.", "$pkg.kotlin.") {
        exclude("%regex[.+\\.kotlin_builtins]") // Fix issues with kotlin-reflect
    }
    kotlinRelocate("kotlinx.", "$pkg.kotlinx.")
    kotlinRelocate("org.jetbrains.exposed.", "$pkg.org.jetbrains.exposed.")
    kotlinRelocate("org.incendo", "$pkg.org.incendo")
    relocate("com.zaxxer", "$pkg.com.zaxxer")
    relocate("org.spongepowered", "$pkg.org.spongepowered")
    relocate("net.kyori.option", "$pkg.net.kyori.option")

    relocate("info.debatty", "$pkg.info.debatty")
    relocate("org.bstats", "$pkg.org.bstats")
    relocate("de.tr7zw.changeme.nbtapi", "$pkg.nbtapi")

    mergeServiceFiles()

    project(":bukkit:version").subprojects.forEach {
        from(it.sourceSets.main.get().output)
    }
}

tasks.processResources {
    val keys = listOf("versionName", "versionChannel", "versionId")
    val properties = rootProject.ext.properties.filter { keys.contains(it.key) }
    inputs.properties(properties)
    filter<ReplaceTokens>("tokens" to properties)
}

buildConfig {
    buildConfigField("PLUGIN_PLATFORM", "bukkit")
}


allprojects {
    repositories {
        maven("https://jitpack.io")
    }
}