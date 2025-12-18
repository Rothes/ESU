import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
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
        maven("https://jitpack.io")
    }

    dependencies {
        compileOnly(rootProject.libs.packetevents.spigot)
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.add("-java-parameters") // Fix cloud-annotations
        }
    }

}

dependencies {
    paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
    api(project(":common"))
    api(project(":bukkit:module:bukkit-modules-bom"))
    compileOnly(project(":bukkit:version:base"))
    compileOnlyApi(project(":bukkit:dep-bukkit", configuration = "shadow"))

    compileOnlyApi("org.incendo:cloud-paper:2.0.0-beta.13")

    compileOnly("info.debatty:java-string-similarity:2.0.0")

    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")
    compileOnly("net.neoforged:AutoRenamingTool:2.0.13")

    compileOnly("com.rylinaux:PlugManX:2.4.1")
    compileOnly("com.rylinaux:plugman-bukkit:3.0.2") {
        exclude("com.tcoded", "FoliaLib")
    }

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

    compileOnly("com.hankcs:aho-corasick-double-array-trie:1.2.2")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

val fileName = "${rootProject.name}-${project.name}"
tasks.shadowJar {
    archiveFileName = "${fileName}-${project.version}.jar"

    project(":bukkit:version").subprojects.forEach {
        from(it.tasks.shadowJar) {
            into("esu_minecraft_versions")
        }
    }
}

tasks.processResources {
    inputs.property("finalVersionName", finalVersionName)
    filter<ReplaceTokens>("tokens" to mapOf("finalVersionName" to finalVersionName))
}

buildConfig {
    buildConfigField("PLUGIN_PLATFORM", "bukkit")
    buildConfigField("DEP_VERSION_NBTAPI", rootProject.libs.versions.nbt.api)
}

modrinth {
    loaders = listOf("bukkit", "spigot", "paper", "purpur", "folia")
    dependencies {
        optional.project("PlaceholderAPI")
        optional.project("PlugManX")
        optional.project("PacketEvents")
    }
}
