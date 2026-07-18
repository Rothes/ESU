plugins {
    `esu-publishing`
}

esuPublishing {
    artifactIdOverride = "esu-bukkit-remapped"
}

dependencies {
    compileOnlyApi(project(":bukkit:version:remapped-api"))
}