package io.github.rothes.esu.bukkit.util.version.remapper

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.DataSerializer.deserialize
import io.github.rothes.esu.core.util.artifact.local.FileHashes
import io.github.rothes.esu.core.util.artifact.local.FileHashes.Companion.sha1
import net.neoforged.art.api.Renamer
import net.neoforged.art.api.SignatureStripperConfig
import net.neoforged.art.api.Transformer
import net.neoforged.srgutils.IMappingFile
import org.bukkit.Bukkit
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.jar.JarFile

object MappingsLoader {

    private val version = ServerCompatibility.serverVersion
    val hasSpigotMembers = version < "1.18"

    private val cacheFolder = EsuBootstrap.instance.baseConfigPath().resolve(".cache/mappings/$version").toFile()
    private val fileHashes = FileHashes(cacheFolder)

    private const val SERVER_CL = "serverCl.jar"
    private const val SERVER_MOJMAP = "serverMojmap.jar"

    val craftBukkitPackage =
        "org\\.bukkit\\.craftbukkit\\.([^.]+)\\.CraftServer".toRegex()
            .matchEntire(Bukkit.getServer().javaClass.canonicalName)
            ?.groupValues[1]

    val loadedFiles = {
        val mappings = loadMappings()
        val servers = loadServers(mappings)
        CachedFiles(mappings, servers)
    }()

    fun mappingsHash() = fileHashes.dataFile.sha1

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

        EsuBootstrap.instance.info("Remapping server jars")
        val serverMojmap = cacheFolder.resolve(SERVER_MOJMAP)
        Renamer.builder().apply {
            add(Transformer.renamerFactory(mappings.mojmap.reverse(), false))
            add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
            threads(1)
            logger {
                EsuBootstrap.instance.info("[Remapper] $it")
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
                    EsuBootstrap.instance.info("[Remapper] $it")
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
        EsuBootstrap.instance.info("Downloading mappings, this might take a while as it's the first run")
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
        cacheFolder.mkdirs()
        fileHashes.clear()
        files.forEach {
            val file = cacheFolder.resolve(it.first)
            var trys = 0
            while (true) {
                try {
                    URI.create(it.second).toURL().openStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    break
                } catch (e: IOException) {
                    if (trys < 2) {
                        EsuBootstrap.instance.info("Failed to download, retrying: $e")
                        trys++
                    } else {
                        throw IOException("Failed to download ${it.second} to $file", e)
                    }
                }
            }
        }
        if (MappingsLoader.version.minor <= 16) {
            // Mappings doesn't contain NMS package, fixing it
            val prefix = "net/minecraft/server/$craftBukkitPackage/"
            fun String.prefixed() = "$prefix${substringAfterLast('/')}"

            with(cacheFolder.resolve("bukkit-cl.csrg")) {
                writeText(
                    readLines().joinToString("\n") { line ->
                        if (!line.startsWith('#')) {
                            val split = line.split(' ')
                            require(split.size == 2) { "Invalid line format: $line" }
                            "${split[0]} ${split[1].prefixed()}"
                        } else {
                            line
                        }
                    } + "\nnet/minecraft/server/MinecraftServer net/minecraft/server/$craftBukkitPackage/MinecraftServer"
                )
            }
            with(cacheFolder.resolve("bukkit-members.csrg")) {
                val regex = "L([^;)]+)".toRegex()
                fun String.handleArgs() = replace(regex) {
                    if (it.groupValues[1].contains('/') && !it.groupValues[1].startsWith("net/minecraft/server/"))
                        // If it's neither "net/minecraft/server/Main" or "net/minecraft/server/MinecraftServer"
                        it.value
                    else
                        "L${it.groupValues[1].prefixed()}"
                }

                writeText(
                    readLines().joinToString("\n") { line ->
                        if (!line.startsWith('#')) {
                            val split = line.split(' ')
                            require(split.size in 3..4) { "Invalid line format: $line" }
                            when (split.size) {
                                3 -> "${split[0].prefixed()} ${split[1].handleArgs()} ${split[2]}"
                                4 -> "${split[0].prefixed()} ${split[1]} ${split[2].handleArgs()} ${split[3]}"
                                else -> error("?")
                            }
                        } else {
                            line
                        }
                    }
                )
            }
        }
        files.forEach {
            fileHashes.store(cacheFolder.resolve(it.first))
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
        )

        data class Servers(
            val server: File,
            val serverCl: File?,
            val serverMojmap: File,
        )
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