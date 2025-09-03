import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject


val Project.latestCommitHash
    get() = runGitCommand("rev-parse --short HEAD")
val Project.latestCommitMessage
    get() = runGitCommand("log -1 --pretty=%B")

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
        execOperations.exec {
            commandLine = command
            standardOutput = output
        }

        return output.toString(Charsets.UTF_8).trim().ifBlank { "unknown" }
    }
}