plugins {
    id("io.papermc.paperweight.userdev")
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

    val isBase = name == "base"

    val serverVer = if (isBase)
        rootProject.property("targetMinecraftVersion").toString()
    else
        project.name.substring(1).replace('_', '.')

    dependencies {
        paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
        if (!isBase) {
            compileOnly(project(":bukkit:version:base"))
            compileOnly(project(":bukkit"))
        }
    }

    tasks.shadowJar {
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
            exclude(dependency("org.jetbrains:annotations"))
        }
    }
}