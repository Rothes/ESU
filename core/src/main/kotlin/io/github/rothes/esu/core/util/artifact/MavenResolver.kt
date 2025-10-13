package io.github.rothes.esu.core.util.artifact

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.rothes.esu.core.BuildConfig
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.JvmUtils
import io.github.rothes.esu.core.util.NetworkUtils.uriLatency
import io.github.rothes.esu.core.util.artifact.injector.ReflectURLInjector
import io.github.rothes.esu.core.util.artifact.injector.URLInjector
import io.github.rothes.esu.core.util.artifact.injector.UnsafeURLInjector
import io.github.rothes.esu.core.util.extension.listOfJvm
import io.github.rothes.esu.core.util.extension.mapJvm
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.resolution.DependencyResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.InaccessibleObjectException
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import kotlin.io.path.exists
import kotlin.jvm.java

object MavenResolver {

    private val loaded = mutableSetOf<String>()

    private val repository: RepositorySystem = MavenRepositorySystemUtils.newServiceLocator().apply {
        addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
    }.getService(RepositorySystem::class.java)
    private val session: RepositorySystemSession = MavenRepositorySystemUtils.newSession().apply {
        setSystemProperties(System.getProperties())
        checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
        localRepositoryManager = repository.newLocalRepositoryManager(this, LocalRepository("libraries"))
        transferListener = object : AbstractTransferListener() {
            override fun transferStarted(event: TransferEvent) {
                EsuBootstrap.instance.info("Downloading " + event.resource.repositoryUrl + event.resource.resourceName)
            }
        }
        setReadOnly()
    }
    private val repositories: List<RemoteRepository> =
        repository.newResolutionRepositories(session, createRepositories())

    private var injecter: URLInjector = UnsafeURLInjector

    private fun createRepositories(): List<RemoteRepository> {
        val repo = loadRepoConfiguration()
        return mutableListOf<RemoteRepository>().apply {
            add(RemoteRepository.Builder(repo.id, "default", repo.url).build())
            @Suppress("ReplaceCallWithBinaryOperator") // Pure Java support
            if (repo.id.equals("central")) {
                add(RemoteRepository.Builder("NeoForged", "default", "https://maven.neoforged.net/releases/").build())
            }
        }
    }

    fun loadKotlin() {
        loadDependency("org.jetbrains.kotlin:kotlin-reflect:${BuildConfig.KOTLIN_VERSION}")
    }

    fun loadUrl(url: URL) {
        try {
            injecter.addURL(url)
        } catch (e: InaccessibleObjectException) {
            if (injecter == UnsafeURLInjector) {
                injecter = ReflectURLInjector
                injecter.addURL(url)
            } else {
                throw e
            }
        }
    }

    fun loadDependencies(libraries: List<String>, loader: (File, Artifact) -> File = { f, _ -> f }) {
        require(libraries.isNotEmpty()) { "Library must not be empty" }
        for (lib in libraries) {
            loadDependency(lib, loader)
        }
    }

    fun loadDependency(library: String) {
        val result = resolveDependency(library)

        for (it in result.artifactResults) {
            val artifact = it.artifact
            val file = artifact.file
            val url = file.toURI().toURL()
            loadUrl(url)

            val str = "${artifact.groupId}:${artifact.artifactId}"
            loaded.add(str)
        }
    }

    fun loadDependency(library: String, loader: (File, Artifact) -> File = { f, _ -> f }) {
        val result = resolveDependency(library)

        for (it in result.artifactResults) {
            val artifact = it.artifact
            val file = artifact.file
            val toLoad = loader(file, artifact)
            val url = toLoad.toURI().toURL()
            loadUrl(url)

            val str = "${artifact.groupId}:${artifact.artifactId}"
            loaded.add(str)
        }
    }

    private fun resolveDependency(library: String): DependencyResult {
        val dependency = Dependency(DefaultArtifact(library), null)
        var result: DependencyResult
        var trys = 0
        while (true) {
            try {
                result = repository.resolveDependencies(
                    session, DependencyRequest(
                        CollectRequest(null as Dependency?, listOfJvm(dependency), repositories),
                        PatternExclusionsDependencyFilter(loaded.mapJvm { "$it::" })
                    )
                )
                break
            } catch (e: DependencyResolutionException) {
                if (trys < 2)
                    trys++
                else
                    throw RuntimeException("Error resolving libraries", e)
            }
        }
        return result
    }

    fun testDependency(library: String, scope: () -> Unit) {
        try {
            scope()
        } catch (_: Throwable) {
            loadDependencies(listOf(library))
        }
    }

    private fun loadRepoConfiguration(): MavenRepo {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val file = EsuBootstrap.instance.baseConfigPath().resolve("maven-repo.json")
        fun useBest(): MavenRepo {
            val best = findBestMavenRepo()
            saveRepoConfiguration(gson, file, best)
            return best
        }
        if (!file.exists()) {
            return useBest()
        }
        val repo = gson.fromJson(InputStreamReader(Files.newInputStream(file), Charset.forName("UTF-8")), MavenRepo::class.java)
        return repo ?: useBest()
    }

    private fun saveRepoConfiguration(gson: Gson, file: Path, repo: MavenRepo) {
        val writer = OutputStreamWriter(Files.newOutputStream(file), Charset.forName("UTF-8"))
        try {
            writer.append(buildString {
                append("// The Maven repository to download dependencies from. DO NOT change if you don't know about it.\n")
                append("// Delete this file will re-run the latency test, and select the best automatically.\n")
                append(gson.toJson(repo))
            })
        } finally {
            writer.close()
        }
    }

    private fun findBestMavenRepo(): MavenRepo {
        EsuBootstrap.instance.info("Running latency test of maven repositories...")
        val def = MavenRepo("central", "https://maven-central.storage-download.googleapis.com/maven2/")
        val repos = listOfJvm(
            MavenRepo("aliyun", "https://maven.aliyun.com/repository/public/"),
            def,
            MavenRepo("central", "https://maven-central-eu.storage-download.googleapis.com/maven2/"),
            MavenRepo("central", "https://maven-central-asia.storage-download.googleapis.com/maven2/"),
        )
        data class TestResult(val repo: MavenRepo, val latency: Long)
        // Pure java support, on bootstrap stage
        val tested = repos.mapJvm { TestResult(repo = it, latency = it.url.uriLatency) }
        fun compareLatency(it: TestResult) = if (it.latency >= 0) it.latency else Long.MAX_VALUE
        Collections.sort(tested, Comparator { a, b ->
            JvmUtils.compareLong(compareLatency(a), compareLatency(b))
        })
        for ((repo, latency) in tested) {
            EsuBootstrap.instance.info("'${repo.url}': ${latency}ms")
        }
        val best = tested[0]
        return if (best.latency != Long.MAX_VALUE) best.repo else def
    }

    data class MavenRepo(
        val id: String,
        val url: String,
    )

}