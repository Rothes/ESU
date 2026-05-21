/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

val Project.lastTag
    get() = runGitCommand("describe --tags --abbrev=0")
val Project.commitsSinceLastTag
    get() = runGitCommand("rev-list $lastTag..HEAD --count")

val Project.latestCommitHash
    get() = runGitCommand("rev-parse --short HEAD")
val Project.latestCommitMessage
    get() = runGitCommand("log -1 --pretty=%B")

val Project.isRelease
    get() = !(project.version as String).contains('-')

val Project.finalVersionName
    get() = if (isRelease) project.version as String else "${project.version.toString().replace("-SNAPSHOT", "-dev")}-${rootProject.commitsSinceLastTag}"

val Project.lastCommit: Commit
    get() = Commit(latestCommitHash, latestCommitMessage)

fun Project.logSinceCommit(commitHash: String): List<Commit> {
    val raw = runGitCommand("log $commitHash..HEAD --reverse --pretty=format:%x01%h%n%B")
    val map = raw.removePrefix("\u0001") // Remove the char at first commit
        .split('\u0001').map { commit ->
            val split = commit.split('\n', limit = 2)
            val hash = split[0]
            val message = split[1].trimEnd() // Trim the new line each before last commit
            Commit(hash, message)
        }
    return map
}

data class Commit(
    val hashShort: String,
    val message: String,
)

private fun Project.runGitCommand(arg: String): String {
    return providers.of(GitCommand::class.java) { parameters.arg.set(arg) }.get()
}

abstract class GitCommand : ValueSource<String, GitCommand.GitCommandParameters> {

    @get:Inject
    abstract val execOperations: ExecOperations

    interface GitCommandParameters : ValueSourceParameters {
        val arg: Property<String>
    }

    override fun obtain(): String {
        val command = listOf("git") + parameters.arg.get().split(' ')
        val output = ByteArrayOutputStream()
        try {
            execOperations.exec {
                commandLine = command
                standardOutput = output
                errorOutput = output
            }
        } catch (e: Throwable) {
            throw RuntimeException("""
                Failed to run git command ${parameters.arg.get()}
                Output:
                ${output.toString(Charsets.UTF_8).trimIndent()}
            """.trimIndent(), e)
        }

        return output.toString(Charsets.UTF_8).trim().ifBlank { "unknown" }
    }
}
