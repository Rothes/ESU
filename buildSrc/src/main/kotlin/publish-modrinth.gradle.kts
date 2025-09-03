import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar

plugins {
    id("com.modrinth.minotaur")
}

project.modrinth {
    val projectVersion = project.version as String
    val isRelease = !projectVersion.contains('-')
    val modrinthVersion = if (isRelease) projectVersion else "$projectVersion+${System.getenv("GITHUB_RUN_NUMBER")}"
    val changelogContent = if (isRelease) {
        "Changelog editing..."
    } else {
        val commitHash = rootProject.latestCommitHash
        "[$commitHash](https://github.com/Rothes/ESU/commit/$commitHash) ${rootProject.latestCommitMessage}"
    }

    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("ESU")
    versionNumber.set(modrinthVersion)
    versionName.set("ESU-${project.name} $modrinthVersion")
    changelog.set(changelogContent)
    versionType.set(if (isRelease) "release" else "alpha")
    gameVersions = listOf(
        "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21",
        "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", "1.20",
        "1.19.4",
        "1.18.2",
        "1.17.1",
        "1.16.5"
    )
    uploadFile.set(tasks.shadowJar)
}