import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    `esu-publishing`
    id("io.papermc.paperweight.userdev")
}

dependencies {
    compileOnly(project(":common"))
    val serverVer = rootProject.property("targetMinecraftVersion").toString()
    val paperweight = extensions.getByName<PaperweightUserDependenciesExtension>("paperweight")
    paperweight.paperDevBundle("$serverVer.build.+")
}

esuPublishing {
    artifactIdOverride = "esu-bukkit-common"
}