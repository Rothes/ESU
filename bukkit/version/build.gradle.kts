import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension
import io.papermc.paperweight.userdev.internal.setup.UserdevSetupTask

plugins {
    `no-build-dir`
}

subprojects {
    apply(plugin = "io.papermc.paperweight.userdev")

    val isRemapped = name == "remapped"

    val serverVer = if (isRemapped)
        rootProject.property("targetMinecraftVersion").toString()
    else
        project.name.substring(1).replace('_', '.')
    val devBundle = if (serverVer.startsWith('1')) "$serverVer-R0.1-SNAPSHOT" else "$serverVer.build.+"

    dependencies {
        val paperweight = extensions.getByName<PaperweightUserDependenciesExtension>("paperweight")
        paperweight.paperDevBundle(devBundle)
        compileOnly(project(":common"))
        compileOnly(project(":bukkit:bukkit-api"))
        compileOnly(project(":bukkit:module:bukkit-modules-bom"))
        if (!isRemapped) {
            compileOnly(project(":bukkit"))
            compileOnly(project(":bukkit:version:remapped"))
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

    tasks.getByName<UserdevSetupTask>("paperweightUserdevSetup") {
        launcher = javaToolchainService.launcherFor {
            // Set Java version for paperweight to remap setup
            languageVersion.set(
                JavaLanguageVersion.of(
                    if (serverVer.startsWith("26")) 25 // Java 25 since 26.1
                    else if (serverVer.startsWith("1.2")) 21 // Java 21 since 1.20.5
                    else 17 // For 1.18 we must use Java 17
                )
            )
        }
    }

    if (isRemapped) {
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