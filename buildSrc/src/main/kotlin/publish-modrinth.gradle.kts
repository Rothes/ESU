import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

plugins {
    id("com.modrinth.minotaur")
}

private val projectVersion
    get() = project.version as String
private val isRelease
    get() = !projectVersion.contains('-')
private val versionValue
    get() = if (isRelease) projectVersion else "$projectVersion-b${rootProject.commitsSinceLastTag}"

project.modrinth {
    val changelog = if (isRelease) {
        "Changelog waiting for edit..."
    } else {
        val commitHash = rootProject.latestCommitHash
        "[$commitHash](https://github.com/Rothes/ESU/commit/$commitHash): ${rootProject.latestCommitMessage}"
    }
    val versionName = "ESU-${project.name} $versionValue"

    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("ESU")
    this.versionNumber.set(versionValue)
    this.versionName.set(versionName)
    this.changelog.set(changelog)
    versionType.set(if (isRelease) "release" else "alpha")
    gameVersions = listOf(
        "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8",
        "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
        "1.19.4",
        "1.18.2",
        "1.17.1",
        "1.16.5"
    )
    uploadFile.set(tasks.shadowJar)
}

tasks.register("editChangelog") {
    val versionNumber = System.getenv("GIT_TAG") ?: versionValue
    val client = OkHttpClient.Builder().build()
    val response = client.newCall(
        Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url("https://api.modrinth.com/v2/project/esu/version")
            .get()
            .build()
    ).execute().use { it.body!!.string() }

    val token = System.getenv("MODRINTH_TOKEN") ?: error("No token set")
    val changelog = System.getenv("CHANGELOG_CONTENT") ?: error("No changelog set")
    val json = JsonObject().apply {
        addProperty("changelog", changelog)
    }.toString()
    val body = json.toRequestBody("application/json".toMediaType())
    val versions = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        .fromJson<List<Version>>(response, object : TypeToken<List<Version>>() {}.type)
        .filter { it.versionNumber == versionNumber }
    for (version in versions) {
        client.newCall(
            Request.Builder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", token)
                .url("https://api.modrinth.com/v2/version/${version.id}")
                .patch(body)
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to modify version ${version.id}: ${response.body?.string()}")
            }
        }
    }
}

data class Version(
    val id: String,
    val versionNumber: String,
)