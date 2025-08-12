package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.meta.*
import io.github.rothes.esu.core.configuration.serializer.*
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.lib.org.spongepowered.configurate.BasicConfigurationNode
import io.github.rothes.esu.lib.org.spongepowered.configurate.CommentedConfigurationNode
import io.github.rothes.esu.lib.org.spongepowered.configurate.CommentedConfigurationNodeIntermediary
import io.github.rothes.esu.lib.org.spongepowered.configurate.ConfigurationNode
import io.github.rothes.esu.lib.org.spongepowered.configurate.loader.HeaderMode
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.ObjectMapper
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.NodeResolver
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.Processor
import io.github.rothes.esu.lib.org.spongepowered.configurate.util.MapFactories
import io.github.rothes.esu.lib.org.spongepowered.configurate.yaml.NodeStyle
import io.github.rothes.esu.lib.org.spongepowered.configurate.yaml.YamlConfigurationLoader
import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.Component
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipException
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull

object ConfigLoader {

    private val serverAdventure = try {
        Component.text()
        true
    } catch (_: Throwable) {
        false
    }

    inline fun <reified T: MultiConfiguration<D>, reified D: ConfigurationPart>
            loadMulti(path: Path, vararg forceLoad: String,
                      create: Array<String>? = null, loadSubDir: Boolean = false,
                      nameMapper: (Path) -> String = { it.nameWithoutExtension },
                      builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
                      modifier: (D, Path) -> D = { it, _ -> it }): T {
        return loadMulti(path, D::class.java, forceLoad = forceLoad, create, loadSubDir, nameMapper, builder, modifier)
    }

    inline fun <reified T: MultiConfiguration<D>, D: ConfigurationPart>
            loadMulti(path: Path, clazz: Class<D>, vararg forceLoad: String,
                      create: Array<String>? = null, loadSubDir: Boolean = false,
                      nameMapper: (Path) -> String = { it.nameWithoutExtension },
                      builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
                      modifier: (D, Path) -> D = { it, _ -> it }): T {
        if (clazz.isInstance(EmptyConfiguration)) {
            return T::class.java.getConstructor(Map::class.java).newInstance(emptyMap<String, D>())
        }
        if (MultiLocaleConfiguration::class.java.isAssignableFrom(T::class.java) && path.notExists()) {
            EsuConfig.get().localeSoftLinkPath.getOrNull()?.let { linkTo ->
                val relativize = EsuCore.instance.baseConfigPath().relativize(path)
                val source = linkTo.resolve(relativize)
                if (!source.isDirectory()) {
                    source.createDirectories()
                }
                Files.createSymbolicLink(path, source)
                EsuCore.instance.info("Created symbolic link: [$path] -> [$source]")
            }
        }
        return T::class.java.getConstructor(Map::class.java).newInstance(
            buildMap {
                val files = forceLoad.map { path.resolve(it) }.toMutableSet()
                if (create?.isNotEmpty() == true && path.notExists()) {
                    files.addAll(create.map { path.resolve(it) })
                }

                if (path.isDirectory()) {
                    loadDirectory(path, files, loadSubDir)
                }
                files.forEach { file ->
                    put(nameMapper(file), load(file, clazz, builder, modifier))
                }
            }
        )
    }

    fun loadDirectory(dir: Path, files: MutableCollection<Path>, deep: Boolean) {
        dir.forEachDirectoryEntry {
            if (it.isRegularFile() && !files.contains(it)) {
                files.add(it)
            } else if (it.isDirectory() && deep) {
                loadDirectory(it, files, true)
            }
        }
    }

    inline fun <reified T> load(path: Path,
                                builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
                                modifier: (T, Path) -> T = { it, _ -> it }): T {
        return load(path, T::class.java, builder, modifier)
    }

    inline fun <T> load(path: Path, clazz: Class<T>,
                        builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
                        modifier: (T, Path) -> T = { it, _ -> it }): T {
        if (clazz.isInstance(EmptyConfiguration)) {
            return clazz.cast(EmptyConfiguration)
        }
        if (path.isDirectory()) {
            throw IllegalArgumentException("Path '$path' is a directory")
        }
        val loader = createBuilder().path(path).let(builder).build()
        val node = loader.load()
        val t = modifier.invoke(node.require(clazz), path)
        node.set(clazz, t)
        loader.save(node)
        if (t is SavableConfiguration) {
            t.path = path
        }
        return t
    }

    private fun debugNodeSerialization(node: ConfigurationNode, clazz: Type) {
        val serial = node.options().serializers().get(clazz)!!
        println(serial)
        println(serial.emptyValue(clazz, node.options()))
        println((serial as ObjectMapper.Factory).get(clazz).load(BasicConfigurationNode.root(node.options().shouldCopyDefaults(false))))
    }

    inline fun save(path: Path, obj: Any,
                        builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it }) {
        if (path.isDirectory()) {
            throw IllegalArgumentException("Path '$path' is a directory")
        }
        val loader = createBuilder().path(path).let(builder).build()
        val node = loader.load() ?: CommentedConfigurationNode.root(loader.defaultOptions())
        node.set(obj.javaClass, obj)
        loader.save(node)
    }

    fun createBuilder(): YamlConfigurationLoader.Builder {
        return YamlConfigurationLoader.builder()
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .headerMode(HeaderMode.PRESERVE)
            .lineLength(150)
            .commentsEnabled(true)
            .defaultOptions { options ->
                val factory: ObjectMapper.Factory = ObjectMapper.factoryBuilder()
                    .addProcessor(Comment::class.java) { data, _ ->
                        Processor { _, destination ->
                            if (destination is CommentedConfigurationNodeIntermediary<*>) {
                                if (data.overrideOld.isEmpty() || destination.comment() == null) {
                                    destination.commentIfAbsent(data.value.trimIndent())
                                } else if (data.overrideOld.first() == OVERRIDE_ALWAYS) {
                                    destination.comment(data.value.trimIndent())
                                } else if (data.overrideOld.map { it.trimIndent() }.contains(destination.comment())) {
                                    destination.comment(data.value.trimIndent())
                                }
                            }
                        }
                    }
                    .addProcessor(NoDeserializeIf::class.java) { data, _ ->
                        Processor { value, destination ->
                            if (value.toString() == data.value) {
//                                destination.parent()?.removeChild(destination.key())
                                destination.set(null)
                            }
                        }
                    }
                    .addProcessor(RemovedNode::class.java) { _, _ ->
                        Processor { _, destination ->
                            var root = destination
                            while (true) {
                                root = root.parent() ?: break
                            }
                            val node = root.node("old-config-removed", *destination.path().array())
                            node.from(destination)
                            destination.parent()?.removeChild(destination.key())
                        }
                    }
                    .addProcessor(RenamedFrom::class.java) { data, _ ->
                        val name = data.oldName
                        require(name.isNotEmpty())
                        Processor { _, destination ->
                            val parent = if (name.startsWith('/')) {
                                var root = destination
                                while (true) {
                                    root = root.parent() ?: break
                                }
                                root
                            } else {
                                var p = destination.parent()!!
                                for (i in 0 ..< name.count { it == '.' }) {
                                    p = p.parent() ?: error("Parent node is null while processing $name")
                                }
                                p
                            }
                            val from = parent.node(*name.split('.').toTypedArray())
                            if (!from.virtual()) {
                                destination.set(from.raw())
                                from.set(null)
                                from.parent()!!.removeChild(from.key())
                            }
                        }
                    }
                    .addNodeResolver { name, _ ->
                        // Skip kotlin delegate(e.g. lazy) properties
                        if (name.endsWith("\$delegate")) {
                            NodeResolver.SKIP_FIELD
                        } else null
                    }
                    .build()
                options.mapFactory(MapFactories.insertionOrdered())
                    .serializers {
                        if (EsuCore.instance.dependenciesResolved) {
                            it.register(CaptionSerializer)
                                .register(ComponentSerializer)
                                .register(TextColorSerializer)
                        }
                        if (serverAdventure) {
                            it.register(ServerComponentSerializer)
                            it.register(ServerTextColorSerializer)
                        }
                        it.register(
                            { type ->
                                GenericTypeReflector.erase(type).let { clazz ->
                                    ConfigurationPart::class.java.isAssignableFrom(clazz)
                                }
                            }, factory.asTypeSerializer())
                            .registerAnnotatedObjects(factory)
                            .register(TypeToken.get(List::class.java), ListSerializer)
                            .register(TypeToken.get(Map::class.java), MapSerializer)
                            .register(TypeToken.get(Optional::class.java), OptionalSerializer)
                            .register(TypeToken.get(Unit::class.java), EmptySerializer())
                            .register(DurationSerializer)
                            .register(EnumValueSerializer)
                            .register(JavaDurationSerializer)
                            .register(LocalDateSerializer)
                            .register(LocalTimeSerializer)
                            .register(MessageDataSerializer)
                            .register(PathSerializer)
                            .register(RegexSerializer)
                            .register(
                                { type ->
                                    GenericTypeReflector.erase(type).let { clazz ->
                                        try {
                                            clazz.kotlin.isData
                                        } catch (_: KotlinReflectionNotSupportedError) {
                                            // If we don't have kotlin-reflect
                                            false
                                        } catch (e: Throwable) {
                                            if (e is ZipException) {
                                                // ZipException: ZipFile invalid LOC header (bad signature)
                                                // Mostly caused by hot-update
                                                EsuCore.instance.err("Failed to check ${clazz.canonicalName} isData: " + e.message)
                                            } else if (e.javaClass.simpleName != "KotlinReflectionInternalError") {
                                                e.printStackTrace()
                                            }
                                            false
                                        }
                                    }
                                }, factory.asTypeSerializer())
                    }
            }
    }

}