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
    get() = if (isRelease) project.version as String else "${project.version}-${rootProject.commitsSinceLastTag}"

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
                ${output.toString(Charsets.UTF_8)}
            """.trimIndent(), e)
        }

        return output.toString(Charsets.UTF_8).trim().ifBlank { "unknown" }
    }
}