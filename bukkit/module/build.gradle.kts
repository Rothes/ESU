import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    `no-build-dir`
}

subprojects {
    apply(plugin = "esu-publishing")
    apply(plugin = "io.papermc.paperweight.userdev")
    dependencies {
//        compileOnly(project(":bukkit:module:bukkit-bom"))
        compileOnly(project(":common"))
        val serverVer = rootProject.property("targetMinecraftVersion").toString()
        val paperweight = extensions.getByName<PaperweightUserDependenciesExtension>("paperweight")
        paperweight.paperDevBundle("$serverVer.build.+")
    }

    extensions.getByType(Esu_publishing_gradle.EsuPublishingExtension::class.java).apply {
        nameOverride = "esu-bukkit-module-" + project.name.removePrefix("bukkit-")
    }
}
