import com.xpdustry.ksr.kotlinRelocate
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

val serverVer = rootProject.property("targetMinecraftVersion").toString()

allprojects {
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

        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases/")
        }
    }

    dependencies {
        compileOnly("com.github.retrooper:packetevents-spigot:2.9.3")
        compileOnly("org.lz4:lz4-java:1.8.0")
    }

    tasks.shadowJar {
        val pkg = "io.github.rothes.${rootProject.name.lowercase()}.lib"
        fun relocate(pattern: String) {
            relocate(pattern, "$pkg.$pattern")
        }
        kotlinRelocate("kotlin.", "$pkg.kotlin.") {
            exclude("%regex[.+\\.kotlin_builtins]") // Fix issues with kotlin-reflect
        }
        kotlinRelocate("kotlinx.", "$pkg.kotlinx.")
        kotlinRelocate("org.jetbrains.exposed.", "$pkg.org.jetbrains.exposed.")
        kotlinRelocate("org.incendo", "$pkg.org.incendo")
        relocate("com.zaxxer")
        relocate("org.spongepowered")
        relocate("net.kyori.option")

        relocate("info.debatty")
        relocate("org.bstats")
        relocate("de.tr7zw.changeme.nbtapi")

        mergeServiceFiles()
    }
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

    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")
    compileOnly("org.apache.maven:maven-resolver-provider:3.9.6")
    compileOnly("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.18")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.9.18")
    compileOnly("net.neoforged:AutoRenamingTool:2.0.13")

    compileOnly("com.rylinaux:PlugManX:2.4.1")

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

    project(":bukkit:version").subprojects.forEach {
        from(it.tasks.shadowJar) {
            into("esu_minecraft_versions")
        }
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