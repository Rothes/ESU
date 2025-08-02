package io.github.rothes.esu.bukkit.util.version.remapper

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.DataSerializer.deserialize
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.util.version.Version
import net.neoforged.art.api.Renamer
import net.neoforged.art.api.SignatureStripperConfig
import net.neoforged.art.api.Transformer
import net.neoforged.srgutils.IMappingFile
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.jar.JarFile

object MappingsLoader {

    private val version = ServerCompatibility.serverVersion
    val hasSpigotMembers = version < Version.fromString("1.18")

    private val cacheFolder = plugin.dataFolder.resolve(".cache/mappings/$version")
    private val fileHashes = FileHashes(cacheFolder)

    private const val SERVER_CL = "serverCl.jar"
    private const val SERVER_MOJMAP = "serverMojmap.jar"

    val loadedFiles = {
        val mappings = loadMappings()
        val servers = loadServers(mappings)
        CachedFiles(mappings, servers)
    }()

    fun reobf(file: File): File {
        val output = plugin.dataFolder.resolve(".cache/remapped").resolve(file.name)
        output.parentFile.mkdirs()
        val renamer = Renamer.builder().apply {
            loadedFiles.mappings.values.forEach {
                add(Transformer.renamerFactory(it, false))
            }
            add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
            lib(File(plugin.javaClass.protectionDomain.codeSource.location.path))
            loadedFiles.servers.values.forEach {
                lib(it)
            }
            threads(1)
            logger {
                if (it == "Adding Libraries to Inheritance"
                    || it == "Adding input to inheritance map"
                    || it == "Adding extras"
                    || it == "Sorting"
                    || it.startsWith("Conflicting propagated mapping"))
                    return@logger
                plugin.info("[Remapper] $it")
            }
        }.build()
        renamer.run(file, output)
        return output
    }

    private fun loadMappings(): CachedFiles.Mappings {
        return loadMappingsFromCache() ?: let {
            downloadFiles()
            loadMappingsFromCache() ?: error("Failed to load mappings, cache corrupted")
        }
    }

    private fun loadMappingsFromCache(): CachedFiles.Mappings? {
        if (!cacheFolder.isDirectory)
            return null

        fun getMapping(fileName: String): IMappingFile? {
            val file = cacheFolder.resolve(fileName)
            if (!file.isFile || !fileHashes.verify(file))
                return null
            return IMappingFile.load(file)
        }

        return CachedFiles.Mappings(
            mojmap =    getMapping("mojang.txt") ?: return null,
            cbCl =      getMapping("bukkit-cl.csrg") ?: return null,
            cbMembers = if (hasSpigotMembers) getMapping("bukkit-members.csrg") ?: return null else null,
        )
    }

    private fun loadServers(mappings: CachedFiles.Mappings): CachedFiles.Servers {
        return loadServersFromCache() ?: let {
            remapServers(mappings)
            loadServersFromCache() ?: error("Failed to load servers, cache corrupted")
        }
    }

    private fun loadServersFromCache(): CachedFiles.Servers? {
        val server = getActualServerJar()
        val serverCl = if (hasSpigotMembers) cacheFolder.resolve(SERVER_CL) else null
        val serverMojmap = cacheFolder.resolve(SERVER_MOJMAP)

        if (!fileHashes.verify(server) || !fileHashes.verify(serverMojmap)
            || (serverCl != null && !fileHashes.verify(serverCl))) {
            return null
        }
        return CachedFiles.Servers(
            server,
            serverCl,
            serverMojmap,
        )
    }

    private fun remapServers(mappings: CachedFiles.Mappings) {
        val server = getActualServerJar()
        fileHashes.store(server)

        plugin.info("Remapping server jars")
        val serverMojmap = cacheFolder.resolve(SERVER_MOJMAP)
        Renamer.builder().apply {
            add(Transformer.renamerFactory(mappings.mojmap.reverse(), false))
            add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
            threads(1)
            logger {
                plugin.info("[Remapper] $it")
            }
        }.build().run(server, serverMojmap)
        fileHashes.store(serverMojmap)

        if (hasSpigotMembers) {
            val serverCl = cacheFolder.resolve(SERVER_CL)
            Renamer.builder().apply {
                add(Transformer.renamerFactory(mappings.cbCl, false))
                add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
                threads(1)
                logger {
                    plugin.info("[Remapper] $it")
                }
            }.build().run(server, serverCl)
            fileHashes.store(serverCl)
        }
        fileHashes.save()
    }

    private fun getActualServerJar(): File {
        val serverJar = cacheFolder.resolve("server.jar")
        val jarFile = JarFile(serverJar)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.startsWith("META-INF/versions/") && entry.name.endsWith(".jar")) {
                val embedded = cacheFolder.resolve("serverEmbedded.jar")
                jarFile.getInputStream(entry).use { input ->
                    embedded.createNewFile()
                    embedded.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return embedded
            }
        }
        return serverJar
    }

    private fun downloadFiles() {
        val commit = getSpigotCommit()
        val version = getMinecraftVersion()
        val pkg = version.packageObject
        val files = buildList {
            add("server.jar" to getServerJarUrl(pkg))
            add("mojang.txt" to getServerMappingsUrl(pkg))
            add("bukkit-cl.csrg" to "https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings/bukkit-${MappingsLoader.version}-cl.csrg?at=$commit")
            if (hasSpigotMembers)
                add("bukkit-members.csrg" to "https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings/bukkit-${MappingsLoader.version}-members.csrg?at=$commit")
        }
        plugin.info("Downloading mappings")
        cacheFolder.mkdirs()
        fileHashes.clear()
        files.forEach {
            val file = cacheFolder.resolve(it.first)
            URI.create(it.second).toURL().openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            fileHashes.store(file)
        }
        fileHashes.save()
    }

    private fun getSpigotCommit(): String {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://hub.spigotmc.org/stash/rest/api/1.0/projects/SPIGOT/repos/builddata/commits?withCounts=true&limit=1000"))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString()).body().deserialize<StashCommits>()
        val commit = response.values.firstOrNull { commit ->
            commit.message == "Update to Minecraft $version"
        } ?: error("Failed to find spigot mapping commit")
        return commit.id
    }

    private fun getMinecraftVersion(): McVersionManifest.Version {
        val response = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json".readUrl().deserialize<McVersionManifest>()
        val version = response.versions.first {
            it.id == version.toString()
        }
        return version
    }

    private fun getServerMappingsUrl(pkg: McVersionPackage): String {
        val artifact = pkg.downloads["server_mappings"] ?: error("Mojang server mappings is not provided")
        return artifact.url
    }

    private fun getServerJarUrl(response: McVersionPackage): String {
        val artifact = response.downloads["server"] ?: error("Server jar is not provided")
        return artifact.url
    }

    private fun String.readUrl(): String = URI.create(this).toURL().readText()

    data class CachedFiles(
        val mappings: Mappings,
        val servers: Servers,
    ) {
        data class Mappings(
            val mojmap: IMappingFile,
            val cbCl: IMappingFile,
            val cbMembers: IMappingFile?,
        ) {
            val values = listOfNotNull(mojmap, cbCl, cbMembers)
        }

        data class Servers(
            val server: File,
            val serverCl: File?,
            val serverMojmap: File,
        ) {
            val values = listOfNotNull(server, serverCl, serverMojmap)
        }
    }

    private data class StashCommits(
        val authorCount: Int,
        val isLastPage: Boolean,
        val limit: Int,
        val nextPageStart: Int,
        val size: Int,
        val start: Int,
        val totalCount: Int,
        val values: List<Value>
    ) {
        data class Value(
            val id: String,
            val message: String,
        )
    }

    private data class McVersionManifest(
        val latest: Latest,
        val versions: List<Version>
    ) {

        data class Latest(
            val release: String,
            val snapshot: String,
        )
        data class Version(
            val id: String,
            val type: String,
            val url: String,
            val sha1: String,
        ) {
            val packageObject: McVersionPackage
                get() = url.readUrl().deserialize<McVersionPackage>()
        }
    }

    private data class McVersionPackage(
        val downloads: Map<String, DownloadObject>
    ) {
        data class DownloadObject(
            val sha1: String,
            val size: Int,
            val url: String,
        )
    }

}