plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

val serverVer = rootProject.property("targetMinecraftVersion").toString()

dependencies {
    paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
    compileOnlyApi(project(":bukkit:dep-bukkit", configuration = "shadow"))
    api(project(":bukkit:module:bukkit-modules-bom"))
    api(project(":common"))

    // Item libraries
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1")
    compileOnly("io.lumine:Mythic-Dist:5.8.0")
    compileOnly("io.lumine:MythicLib-dist:1.7.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.9.5-SNAPSHOT")
    compileOnly("net.momirealms:craft-engine-core:0.0.49")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.49")

    compileOnly("me.clip:placeholderapi:2.11.7")

    compileOnly("fr.xephi:authme:5.6.1-SNAPSHOT")

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