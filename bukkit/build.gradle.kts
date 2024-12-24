import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.gradleup.shadow") version "8.3.4"
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
    maven("https://mvn.lumine.io/repository/maven-public")
}


dependencies {
    paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
    api(project(":core"))
    api("org.incendo:cloud-paper:2.0.0-beta.10")
    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("com.h2database:h2:2.3.232")
    compileOnly("com.rylinaux:PlugManX:2.4.1")

    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1")
    compileOnly("io.lumine:MythicLib-dist:1.5.2-SNAPSHOT")
    compileOnly("io.lumine:Mythic-Dist:5.6.1")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

val fileName = "${rootProject.name}-${project.name}"
tasks.shadowJar {
    archiveFileName = "${fileName}-${project.version}-mojmap.jar"

    relocate("kotlin", "io.github.rothes.esu.lib.kotlin")
    relocate("org.incendo", "io.github.rothes.esu.lib.org.incendo")
    relocate("cc.carm.lib", "io.github.rothes.esu.lib.cc.carm.lib")
    relocate("org.spongepowered", "io.github.rothes.esu.lib.org.spongepowered")
    relocate("info.debatty", "io.github.rothes.esu.lib.info.debatty")
    relocate("org.h2", "io.github.rothes.esu.lib.org.h2")

//    exclude("com.google.errorprone")
}

tasks.processResources {
    filter<ReplaceTokens>(
        "tokens" to mapOf(
            "versionName" to project.property("versionName"),
            "versionChannel" to project.property("versionChannel"),
            "versionId" to project.property("versionId"),
        ))
    outputs.doNotCacheIf("MakeReplacementsWork") { true }
}
