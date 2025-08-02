package io.github.rothes.esu.bukkit.util.artifact

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.artifact.injector.ReflectURLInjector
import io.github.rothes.esu.bukkit.util.artifact.injector.URLInjector
import io.github.rothes.esu.bukkit.util.artifact.injector.UnsafeURLInjector
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
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transport.http.HttpTransporterFactory
import java.lang.reflect.InaccessibleObjectException
import java.net.URL

object MavenResolver {

    private val repository: RepositorySystem = MavenRepositorySystemUtils.newServiceLocator().apply {
        addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
    }.getService(RepositorySystem::class.java)
    private val session: RepositorySystemSession = MavenRepositorySystemUtils.newSession().apply {
        setSystemProperties(System.getProperties())
        setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL)
        setLocalRepositoryManager(repository.newLocalRepositoryManager(this, LocalRepository("libraries")))
        setTransferListener(object : AbstractTransferListener() {
            override fun transferStarted(event: TransferEvent) {
                plugin.info("Downloading " + event.resource.repositoryUrl + event.resource.resourceName)
            }
        })
        setReadOnly()
    }
    private val repositories: List<RemoteRepository> =
        repository.newResolutionRepositories(session, createRepositoriesWithMirrors())

    private var injecter: URLInjector = UnsafeURLInjector

    private fun createRepositoriesWithMirrors(): List<RemoteRepository> {
        return listOf(
            RemoteRepository.Builder("central", "default", "https://maven-central.storage-download.googleapis.com/maven2").build(),
            // Chinese mirrors
            RemoteRepository.Builder("aliyun", "default", "https://maven.aliyun.com/repository/public/").build(),
            RemoteRepository.Builder("huawei", "default", "https://repo.huaweicloud.com/repository/maven/").build(),
            // NeoForged
            RemoteRepository.Builder("NeoForged", "default", "https://maven.neoforged.net/releases/").build(),
        )
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
        val result = try {
            repository.resolveDependencies(
                session, DependencyRequest(CollectRequest(null as Dependency?, dependencies, repositories), null)
            )
        } catch (e: DependencyResolutionException) {
            throw RuntimeException("Error resolving libraries ", e)
        }

        // TODO: Remap
        result.artifactResults.forEach {
            val file = it.artifact.file
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