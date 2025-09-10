package io.github.rothes.esu.core.configuration

import com.google.common.cache.CacheBuilder
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.meta.*
import io.github.rothes.esu.core.configuration.serializer.*
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.util.tree.TreeNode
import io.github.rothes.esu.lib.org.spongepowered.configurate.BasicConfigurationNode
import io.github.rothes.esu.lib.org.spongepowered.configurate.CommentedConfigurationNode
import io.github.rothes.esu.lib.org.spongepowered.configurate.CommentedConfigurationNodeIntermediary
import io.github.rothes.esu.lib.org.spongepowered.configurate.ConfigurationNode
import io.github.rothes.esu.lib.org.spongepowered.configurate.loader.HeaderMode
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.ObjectMapper
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.NodeResolver
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.Processor
import io.github.rothes.esu.lib.org.spongepowered.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.org.spongepowered.configurate.util.MapFactories
import io.github.rothes.esu.lib.org.spongepowered.configurate.yaml.NodeStyle
import io.github.rothes.esu.lib.org.spongepowered.configurate.yaml.YamlConfigurationLoader
import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.Component
import java.io.File
import java.lang.reflect.Type
import java.net.JarURLConnection
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import java.util.zip.ZipException
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull

object ConfigLoader {

    private val langCache = CacheBuilder.newBuilder()
        .expireAfterAccess(8, TimeUnit.HOURS)
        .build<ClassLoader, TreeNode<List<String>>>()

    private val serverAdventure = try {
        Component.text()
        true
    } catch (_: Throwable) {
        false
    }

    private val scalarSerializers = mutableSetOf<ScalarSerializer<*>>()

    fun registerSerializer(serializer: ScalarSerializer<*>) = scalarSerializers.add(serializer)
    fun unregisterSerializer(serializer: ScalarSerializer<*>) = scalarSerializers.remove(serializer)

    inline fun <reified T: MultiConfiguration<D>, reified D: ConfigurationPart>
            loadMulti(path: Path, vararg forceLoad: String): T {
        return loadMultiSimple(path, T::class.java, D::class.java, forceLoad.toList())
    }

    inline fun <reified T: MultiConfiguration<D>, reified D: ConfigurationPart>
            loadMulti(path: Path, forceLoad: List<String>): T {
        return loadMultiSimple(path, T::class.java, D::class.java, forceLoad)
    }

    inline fun <reified T: MultiConfiguration<D>, reified D: ConfigurationPart>
            loadMulti(path: Path, settings: LoaderSettingsMulti<D>): T {
        return loadMulti(path, T::class.java, D::class.java, settings)
    }

    inline fun <reified T: MultiConfiguration<D>, D: ConfigurationPart>
            loadMulti(path: Path, dataClass: Class<D>, settings: LoaderSettingsMulti<D>): T {
        return loadMulti(path, T::class.java, dataClass, settings)
    }

    inline fun <reified T: MultiConfiguration<D>, reified D: ConfigurationPart>
            loadMulti(path: Path, settings: LoaderSettingsMulti.Builder<D>): T {
        return loadMulti(path, T::class.java, D::class.java, settings.build())
    }

    inline fun <reified T: MultiConfiguration<D>, D: ConfigurationPart>
            loadMulti(path: Path, dataClass: Class<D>, settings: LoaderSettingsMulti.Builder<D>): T {
        return loadMulti(path, T::class.java, dataClass, settings.build())
    }

    fun <T: MultiConfiguration<D>, D: ConfigurationPart>
            loadMultiSimple(path: Path, configClass: Class<T>, dataClass: Class<D>, forceLoad: List<String>): T {
        return loadMulti(path, configClass, dataClass, LoaderSettingsMulti(forceLoad))
    }

    fun <T: MultiConfiguration<D>, D: ConfigurationPart>
            loadMulti(path: Path, configClass: Class<T>, dataClass: Class<D>, settings: LoaderSettingsMulti<D>): T {
        if (dataClass.isInstance(EmptyConfiguration)) {
            return configClass.getConstructor(Map::class.java).newInstance(emptyMap<String, D>())
        }
        if (MultiLocaleConfiguration::class.java.isAssignableFrom(configClass) && path.notExists()) {
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
        return configClass.getConstructor(Map::class.java).newInstance(
            buildMap {
                val files = settings.forceLoad?.map { path.resolve(it) }?.toMutableSet() ?: mutableSetOf()
                if (settings.createKeys?.isNotEmpty() == true && path.notExists()) {
                    files.addAll(settings.createKeys.map { path.resolve(it) })
                }

                if (path.isDirectory()) {
                    loadDirectory(path, files, settings.loadSubDirectories)
                }
                files.forEach { file ->
                    put(settings.keyMapper(file), load(file, dataClass, settings))
                }
            }
        )
    }

    private fun loadDirectory(dir: Path, files: MutableCollection<Path>, deep: Boolean) {
        dir.forEachDirectoryEntry {
            if (it.isRegularFile() && !files.contains(it)) {
                files.add(it)
            } else if (it.isDirectory() && deep) {
                loadDirectory(it, files, true)
            }
        }
    }

    inline fun <reified T> load(path: Path): T {
        return loadSimple(path, T::class.java)
    }

    inline fun <reified T> load(path: Path, settings: LoaderSettings<T>): T {
        return load(path, T::class.java, settings)
    }

    inline fun <reified T> load(path: Path, settings: LoaderSettings.Builder<T>): T {
        return load(path, T::class.java, settings.build())
    }

    fun <T> loadSimple(path: Path, clazz: Class<T>): T {
        return load(path, clazz, LoaderSettings())
    }

    fun <T> load(path: Path, clazz: Class<T>, settings: LoaderSettings<T>): T {
        if (clazz.isInstance(EmptyConfiguration)) {
            return clazz.cast(EmptyConfiguration)
        }
        if (path.isDirectory()) {
            throw IllegalArgumentException("Path '$path' is a directory")
        }
        val resourceNode =
            if (settings.findResource) {
                val p = settings.basePath.relativize(path)
                val lang = getLangCache(clazz.classLoader, p.pathString)
                val locale = if (EsuConfig.initialized) EsuConfig.get().locale else Locale.getDefault().language + '_' + Locale.getDefault().country.lowercase()
                val resource = lang.find { it.nameWithoutExtension == locale }
                    ?: lang.firstOrNull { it.nameWithoutExtension.substringBefore('_') == locale.substringBefore('_') }
                resource?.let {
                    val conn = clazz.classLoader.getResource(it.resourcePath)!!.openConnection() as JarURLConnection
                    conn.useCaches = false
                    conn.connect()
                    val reader = conn.getInputStream().bufferedReader()
                    createBuilder(null).source { reader }.let(settings.yamlLoader).build().load()
                }
            } else null
        val loader = createBuilder(resourceNode).path(path).let(settings.yamlLoader).build()
        val node = settings.nodeMapper(loader.load())
        val t = settings.modifier.invoke(node.require(clazz), path)
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

    fun save(path: Path, obj: Any, yamlLoader: YamlConfigurationLoader = createBuilder(null).path(path).build()) {
        if (path.isDirectory()) {
            throw IllegalArgumentException("Path '$path' is a directory")
        }
        val node = yamlLoader.load() ?: CommentedConfigurationNode.root(yamlLoader.defaultOptions())
        node.set(obj.javaClass, obj)
        yamlLoader.save(node)
    }

    fun createBuilder(resourceNode: ConfigurationNode?): YamlConfigurationLoader.Builder {
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
                                val resourceComment = (resourceNode?.node(destination.path()) as? CommentedConfigurationNodeIntermediary<*>)?.comment()
                                if (resourceComment != null && destination.comment() == data.value.trimIndent()) {
                                    destination.comment(resourceComment)
                                } else if (data.overrideOld.isEmpty() || destination.comment() == null) {
                                    destination.commentIfAbsent(resourceComment ?: data.value.trimIndent())
                                } else if (data.overrideOld.first() == OVERRIDE_ALWAYS) {
                                    destination.comment(resourceComment ?: data.value.trimIndent())
                                } else if (data.overrideOld.map { it.trimIndent() }.contains(destination.comment())) {
                                    destination.comment(resourceComment ?: data.value.trimIndent())
                                }
                            }
                        }
                    }
                    .addProcessor(MoveToTop::class.java) { _, _ ->
                        Processor { _, destination ->
                            val node = destination.parent() ?: error("Node ${destination.key()} doesn't have a parent")
                            val children = node.childrenMap()
                            if (children.keys.first() == destination.key()) {
                                // Already at the top
                                return@Processor
                            }
                            val list = children.map { it.key to it.value.copy() }
                            node.set(null)
                            node.node(destination.key()).set("placeholder") // Set it first
                            for ((key, value) in list) {
                                node.node(key).from(value)
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
                        for (serializer in scalarSerializers) {
                            it.register(serializer)
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
                                }, factory.asTypeSerializer()
                            )
                    }.let {
                        if (resourceNode != null) {
                            it.header(resourceNode.options().header())
                        } else it
                    }
            }
    }

    private fun getLangCache(classLoader: ClassLoader, path: String): List<LangResource> {
        val tree = langCache.getIfPresent(classLoader) ?: let {
            val root = TreeNode<List<String>>()
            JarFile(URLDecoder.decode(javaClass.protectionDomain.codeSource.location.path, Charsets.UTF_8)).use { jarFile ->
                val entries = jarFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val fullName = entry.name
                    if (fullName.startsWith("lang/") && fullName.last() != '/') {
                        val split = fullName.substringAfter('/').split('/')
                        val path = split.dropLast(1)
                        val node = root.getOrCreateNode(path)
                        val list = node.value as? MutableList<String> ?: mutableListOf<String>().also { node.value = it }
                        list.add(split.last())
                    }
                }
            }
            langCache.put(classLoader, root)
            root
        }
        val node = tree.getNode(path.split(File.separatorChar))
        return node?.value?.map { LangResource(it, node.path) } ?: listOf()
    }

    private class LangResource(
        val name: String,
        path: String,
    ) {
        val nameWithoutExtension = name.substringBeforeLast('.')
        val resourcePath = "lang/$path$name"
    }

    open class LoaderSettings<T>(
        val yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
        val nodeMapper: (ConfigurationNode) -> ConfigurationNode = { it },
        val modifier: (T, Path) -> T = { it, _ -> it },
        val findResource: Boolean = true,
        val basePath: Path = EsuCore.instance.baseConfigPath(),
    ) {

        class Builder<T> {

            var yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it }
            var nodeMapper: (ConfigurationNode) -> ConfigurationNode = { it }
            var modifier: (T, Path) -> T = { it, _ -> it }
            var findResource: Boolean = true
            var basePath: Path = EsuCore.instance.baseConfigPath()

            fun yamlLoader(yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder): Builder<T> {
                this.yamlLoader = yamlLoader
                return this
            }

            fun nodeMapper(mapper: (ConfigurationNode) -> ConfigurationNode): Builder<T> {
                this.nodeMapper = mapper
                return this
            }

            fun modifier(modifier: (T, Path) -> T): Builder<T> {
                this.modifier = modifier
                return this
            }

            fun findResource(findResource: Boolean): Builder<T> {
                this.findResource = findResource
                return this
            }

            fun basePath(basePath: Path): Builder<T> {
                this.basePath = basePath
                return this
            }

            fun build() = LoaderSettings(yamlLoader, nodeMapper, modifier, findResource, basePath)

        }
    }


    class LoaderSettingsMulti<T>(
        val forceLoad: List<String>? = null,
        val createKeys: List<String>? = null,
        val loadSubDirectories: Boolean = false,
        val keyMapper: (Path) -> String = { it.nameWithoutExtension },
        yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
        nodeMapper: (ConfigurationNode) -> ConfigurationNode = { it },
        modifier: (T, Path) -> T = { it, _ -> it },
        findResource: Boolean = true,
        basePath: Path = EsuCore.instance.baseConfigPath(),
    ): LoaderSettings<T>(yamlLoader, nodeMapper, modifier, findResource, basePath) {

        constructor(
            vararg forceLoad: String,
            create: List<String>? = null,
            loadSubDirectories: Boolean = false,
            yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
            keyMapper: (Path) -> String = { it.nameWithoutExtension },
            nodeMapper: (ConfigurationNode) -> ConfigurationNode = { it },
            modifier: (T, Path) -> T = { it, _ -> it },
            findResource: Boolean = true,
            basePath: Path = EsuCore.instance.baseConfigPath(),
        ): this(forceLoad.toList(), create, loadSubDirectories, keyMapper, yamlLoader, nodeMapper, modifier, findResource, basePath)


        class Builder<T> {

            var forceLoad: List<String>? = null
            var createKeys: List<String>? = null
            var loadSubDirectories: Boolean = false
            var keyMapper: (Path) -> String = { it.nameWithoutExtension }
            var yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it }
            var nodeMapper: (ConfigurationNode) -> ConfigurationNode = { it }
            var modifier: (T, Path) -> T = { it, _ -> it }
            var findResource: Boolean = true
            var basePath: Path = EsuCore.instance.baseConfigPath()

            fun forceLoad(vararg forceLoad: String): Builder<T> {
                this.forceLoad = forceLoad.toList()
                return this
            }

            fun forceLoad(forceLoad: List<String>?): Builder<T> {
                this.forceLoad = forceLoad
                return this
            }

            fun create(create: List<String>?): Builder<T> {
                this.createKeys = create
                return this
            }

            fun loadSubDirectories(loadSubDirectories: Boolean): Builder<T> {
                this.loadSubDirectories = loadSubDirectories
                return this
            }

            fun keyMapper(mapper: (Path) -> String): Builder<T> {
                this.keyMapper = mapper
                return this
            }

            fun yamlLoader(yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder): Builder<T> {
                this.yamlLoader = yamlLoader
                return this
            }

            fun nodeMapper(mapper: (ConfigurationNode) -> ConfigurationNode): Builder<T> {
                this.nodeMapper = mapper
                return this
            }

            fun modifier(modifier: (T, Path) -> T): Builder<T> {
                this.modifier = modifier
                return this
            }

            fun findResource(findResource: Boolean): Builder<T> {
                this.findResource = findResource
                return this
            }

            fun basePath(basePath: Path): Builder<T> {
                this.basePath = basePath
                return this
            }


            fun build() = LoaderSettingsMulti(forceLoad, createKeys, loadSubDirectories, keyMapper, yamlLoader, nodeMapper, modifier, findResource, basePath)

        }
    }

}