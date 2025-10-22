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
    compileOnlyApi(project(":velocity:dep-velocity", configuration = "shadow"))

    compileOnlyApi("org.incendo:cloud-velocity:2.0.0-beta.10")

    compileOnly("org.apache.maven.resolver:maven-resolver-api:1.9.18")

    implementation(libs.bstats.velocity)

    compileOnly("com.github.Rothes.ServerUtils:ServerUtils-Velocity:master-SNAPSHOT") // Official repo is down

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

    relocate("org.bstats", "$pkg.bstats")
}

buildConfig {
    buildConfigField("PLUGIN_PLATFORM", "velocity")
}

modrinth {
    loaders = listOf("velocity")
    dependencies {
        optional.project("serverutils")
    }
}
