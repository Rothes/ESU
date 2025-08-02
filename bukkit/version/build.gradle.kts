plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

dependencies {
    val serverVer = rootProject.property("targetMinecraftVersion").toString()
    paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
}

subprojects {
    apply(plugin = "io.papermc.paperweight.userdev")

    val serverVer = project.name.substring(1).replace('_', '.')

    dependencies {
        paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
        compileOnly(project(":bukkit"))
    }

    tasks.shadowJar {
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
            exclude(dependency("org.jetbrains:annotations"))
        }
    }
}