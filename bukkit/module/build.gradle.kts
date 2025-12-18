import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    `no-build-dir`
}

subprojects {
    apply(plugin = "io.papermc.paperweight.userdev")
    dependencies {
        compileOnly(project(":common"))
        val serverVer = rootProject.property("targetMinecraftVersion").toString()
        val paperweight = extensions.getByName<PaperweightUserDependenciesExtension>("paperweight")
        paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
    }

    publishing {
        repositories {
            mavenLocal()
        }
        publications {
            create<MavenPublication>("mavenJar") {
                from(components["java"])

                artifactId = project.name
                groupId = project.group as String?
                version = project.version as String?
            }
        }
    }
}
