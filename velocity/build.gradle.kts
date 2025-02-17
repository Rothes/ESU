import com.xpdustry.ksr.kotlinRelocate

plugins {
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig") version "5.5.1"
}

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

}


dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
//    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    api(project(":core"))
    api("org.incendo:cloud-velocity:2.0.0-beta.10")
    implementation("com.h2database:h2:2.3.232")
    implementation("com.mysql:mysql-connector-j:8.4.0")

    implementation("org.bstats:bstats-velocity:3.1.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-java-parameters") // Fix cloud-annotations
    }
}

val fileName = "${rootProject.name}-${project.name}"
tasks.shadowJar {
    archiveFileName = "${fileName}-${project.version}.jar"

    kotlinRelocate("kotlin.", "io.github.rothes.esu.lib.kotlin.") {
        exclude("%regex[.+\\.kotlin_builtins]") // Fix issues with kotlin-reflect
    }
    kotlinRelocate("kotlinx.", "io.github.rothes.esu.lib.kotlinx.")
    kotlinRelocate("org.incendo", "io.github.rothes.esu.lib.org.incendo")
    relocate("cc.carm.lib", "io.github.rothes.esu.lib.cc.carm.lib")
    relocate("org.spongepowered", "io.github.rothes.esu.lib.org.spongepowered")

    relocate("org.bstats", "io.github.rothes.esu.lib.org.bstats")
}

buildConfig {
    buildConfigField("VERSION_NAME", project.property("versionName").toString())
    buildConfigField("VERSION_CHANNEL", project.property("versionChannel").toString())
    buildConfigField("VERSION_ID", project.property("versionId").toString())
}
