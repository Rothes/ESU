package io.github.rothes.esu.core.util.artifact

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.util.NetworkUtils.uriLatency
import io.github.rothes.esu.core.util.artifact.injector.ReflectURLInjector
import io.github.rothes.esu.core.util.artifact.injector.URLInjector
import io.github.rothes.esu.core.util.artifact.injector.UnsafeURLInjector
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
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
import java.lang.reflect.InaccessibleObjectException
import java.net.URL

object MavenResolver {

    private val blockedGroupIds = setOf("org.jetbrains.kotlin")

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
                EsuCore.instance.info("Downloading " + event.resource.repositoryUrl + event.resource.resourceName)
            }
        }
        setReadOnly()
    }
    private val repositories: List<RemoteRepository> =
        repository.newResolutionRepositories(session, createRepositories())

    private var injecter: URLInjector = UnsafeURLInjector

    private fun createRepositories(): List<RemoteRepository> {
        val repos = linkedMapOf(
            "https://maven-central.storage-download.googleapis.com/maven2/" to "central",
            "https://maven-central-asia.storage-download.googleapis.com/maven2/" to "central-asia",
            "https://maven.aliyun.com/repository/public/" to "aliyun",
        )
        val best = repos.entries.firstOrNull {
            it.key.uriLatency in 0..125
        } ?: repos.firstEntry()
        return buildList {
            add(RemoteRepository.Builder(best.value, "default", best.key).build())
            if (best.value != "aliyun") {
                add(RemoteRepository.Builder("NeoForged", "default", "https://maven.neoforged.net/releases/").build())
            }
        }
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

    fun loadDependencies(libraries: List<String>) {
        require(libraries.isNotEmpty()) { "Library must not be empty" }
        val dependencies = libraries.map { Dependency(DefaultArtifact(it), null) }
        var result: DependencyResult
        var trys = 0
        while (true) {
            try {
                result = repository.resolveDependencies(
                    session, DependencyRequest(
                        CollectRequest(null as Dependency?, dependencies, repositories),
                        PatternExclusionsDependencyFilter(blockedGroupIds.map { "$it:::" })
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

        // TODO: Maybe remap?
        for (it in result.artifactResults) {
            val artifact = it.artifact
            if (blockedGroupIds.contains(artifact.groupId))
                continue
            val file = artifact.file
            val url = file.toURI().toURL()
            loadUrl(url)
        }
    }

    fun testDependency(library: String, score: () -> Unit) {
        try {
            score()
        } catch (_: Throwable) {
            loadDependencies(listOf(library))
        }
    }

}