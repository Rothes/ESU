import com.xpdustry.ksr.kotlinRelocate

plugins {
    kotlin("kapt")
}

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public/")

}


dependencies {
    // Velocity
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.github.rothes.velocity:velocity-proxy:dev~3.0.0-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.1.114.Final")
    // Project
    api(project(":core"))
    api("org.incendo:cloud-velocity:2.0.0-beta.10")

    implementation("com.h2database:h2:2.3.232")
    implementation("com.mysql:mysql-connector-j:8.4.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3")

    implementation("org.bstats:bstats-velocity:3.1.0")

    compileOnly("com.github.Rothes.ServerUtils:ServerUtils-Velocity:master-SNAPSHOT") // Official repo is down
    compileOnly("com.github.retrooper:packetevents-velocity:2.7.0")
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
    kotlinRelocate("kotlin.", "$pkg.kotlin.") {
        exclude("%regex[.+\\.kotlin_builtins]") // Fix issues with kotlin-reflect
    }
    kotlinRelocate("kotlinx.", "$pkg.kotlinx.")
    kotlinRelocate("org.jetbrains.exposed.", "$pkg.org.jetbrains.exposed.")
    kotlinRelocate("org.incendo", "$pkg.org.incendo")
    relocate("com.zaxxer", "$pkg.com.zaxxer")
    relocate("org.spongepowered", "$pkg.org.spongepowered")
    relocate("net.kyori.option", "$pkg.net.kyori.option")

    relocate("org.bstats", "$pkg.org.bstats")

    mergeServiceFiles()
}

buildConfig {
    buildConfigField("PLUGIN_PLATFORM", "velocity")
}
