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
        if (isBase) {
            compileOnly(project(":common"))
        } else {
            compileOnly(project(":bukkit:version:base"))
            compileOnly(project(":bukkit"))
        }
    }

    configurations {
        all {
            exclude("me.lucko", "spark-paper") // me.lucko:spark-paper:1.10.84 is missing on 1.21.0
        }
    }

    tasks.shadowJar {
        archiveFileName = project.name + ".jar"
        val split = project.name.substring(1).split('_')
        for (i in 1 .. split.size) {
            relocate(
                "org.bukkit.craftbukkit.v${split.take(2).joinToString("_")}_R$i",
                "org.bukkit.craftbukkit"
            )
        }
    }

    if (isBase) {
        publishing {
            repositories {
                mavenLocal()
            }
            publications {
                create<MavenPublication>("mavenJar") {
                    from(components["java"])

                    artifactId = "bukkit-version-${project.name}"
                    groupId = project.group as String?
                    version = project.version as String?
                }
            }
        }
    }
}