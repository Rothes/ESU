import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("io.github.goooler.shadow") version "8.1.8"
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

val serverVer = rootProject.property("targetMinecraftVersion").toString()

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}


dependencies {
    paperweight.foliaDevBundle("$serverVer-R0.1-SNAPSHOT")
    api(project(":core"))
    api("org.incendo:cloud-paper:2.0.0-beta.10")
    implementation("info.debatty:java-string-similarity:2.0.0")
}

val fileName = "${rootProject.name}-${project.name}"
tasks.shadowJar {
    archiveFileName = "${fileName}-${project.version}-mojmap.jar"

    relocate("kotlin", "io.github.rothes.esu.lib.kotlin")
        relocate("org.incendo", "io.github.rothes.esu.lib.org.incendo")
        relocate("cc.carm.lib", "io.github.rothes.esu.lib.cc.carm.lib")
        relocate("info.debatty", "io.github.rothes.esu.lib.info.debatty")

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
