plugins {
    id("io.papermc.paperweight.userdev")
    `no-build-dir`
}

allprojects {
    apply(plugin = "io.papermc.paperweight.userdev")
    dependencies {
        compileOnly(project(":common"))
        val serverVer = rootProject.property("targetMinecraftVersion").toString()
        paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
    }
}

subprojects {
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
