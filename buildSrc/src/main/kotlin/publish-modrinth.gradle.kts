import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar

plugins {
    id("com.modrinth.minotaur")
}

project.modrinth {
    val projectVersion = project.version as String
    val isRelease = !projectVersion.contains('-')

    val versionNumber: String
    val versionName: String
    val changelog: String
    if (isRelease) {
        versionNumber = projectVersion
        versionName = "ESU-${project.name} $versionNumber"
        changelog = "Changelog waiting for edit..."
    } else {
        val buildNumber = rootProject.commitsSinceLastTag
        versionNumber = "$projectVersion-b$buildNumber"
        versionName = "ESU-${project.name} $versionNumber"
        val commitHash = rootProject.latestCommitHash
        changelog = "[$commitHash](https://github.com/Rothes/ESU/commit/$commitHash): ${rootProject.latestCommitMessage}"
    }

    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("ESU")
    this.versionNumber.set(versionNumber)
    this.versionName.set(versionName)
    this.changelog.set(changelog)
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