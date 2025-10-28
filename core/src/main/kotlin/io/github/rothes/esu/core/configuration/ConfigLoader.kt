package io.github.rothes.esu.core.configuration

import com.google.common.cache.CacheBuilder
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.meta.*
import io.github.rothes.esu.core.configuration.serializer.*
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.util.extension.ClassExt.jarFile
import io.github.rothes.esu.core.util.tree.TreeNode
import io.github.rothes.esu.lib.configurate.BasicConfigurationNode
import io.github.rothes.esu.lib.configurate.CommentedConfigurationNode
import io.github.rothes.esu.lib.configurate.CommentedConfigurationNodeIntermediary
import io.github.rothes.esu.lib.configurate.ConfigurationNode
import io.github.rothes.esu.lib.configurate.loader.HeaderMode
import io.github.rothes.esu.lib.configurate.objectmapping.ObjectMapper
import io.github.rothes.esu.lib.configurate.objectmapping.meta.NodeResolver
import io.github.rothes.esu.lib.configurate.objectmapping.meta.Processor
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.configurate.util.MapFactories
import io.github.rothes.esu.lib.configurate.yaml.NodeStyle
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.Component
import java.io.File
import java.lang.reflect.Type
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipException
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull

object ConfigLoader {

    private val langCache = CacheBuilder.newBuilder()
        .expireAfterAccess(8, TimeUnit.HOURS)
        .weakKeys()
        .build<ClassLoader, TreeNode<List<String>>>()

    private val PATCH_FILE_REGEX = "([^.]+)\\.patch\\.([^.]+).*".toRegex()

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
        if (MultiLangConfiguration::class.java.isAssignableFrom(configClass)) {
            if (path.notExists()) {
                EsuConfig.get().localeSoftLinkPath.getOrNull()?.let { linkTo ->
                    val relativize = EsuCore.instance.baseConfigPath().relativize(path)
                    val source = linkTo.resolve(relativize)
                    if (!source.isDirectory()) {
                        source.createDirectories()
                    }
                    Files.createSymbolicLink(path, source)
                    EsuCore.instance.info("Created symbolic link: [$path] -> [$source]")
                } ?: path.createDirectories()
            }
            if (settings.findResource) {
                val p = settings.basePath.relativize(path)
                val lang = getLangCache(dataClass, p.pathString)
                for (resource in lang) {
                    val resolve = path.resolve(resource.name)
                    if (resolve.notExists()) {
                        resource.save(dataClass, resolve)
                    } else {
                        val loader = createBuilder(null).let(settings.yamlLoader).path(resolve).build()
                        val config = loader.load()
                        val read = resource.readConfig(dataClass, settings)
                        config.mergeFrom(read)
                        loader.save(config)
                    }
                }
            }
        }
        return configClass.getConstructor(Map::class.java).newInstance(
            buildMap {
                val files = settings.forceLoadConfigs?.map { path.resolve("$it.yml") }?.toMutableSet() ?: mutableSetOf()
                if (settings.initializeConfigs?.isNotEmpty() == true && path.notExists()) {
                    files.addAll(settings.initializeConfigs.map { path.resolve("$it.yml") })
                }

                if (path.isDirectory()) {
                    loadDirectory(path, files, settings.loadSubDirectories)
                }
                files.forEach { file ->
                    val configKey = settings.configKeyMapper(file)
                    put(configKey, load(file, dataClass, settings, configKey))
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

    fun <T> load(path: Path, clazz: Class<T>, settings: LoaderSettings<T>, configKey: String = ""): T {
        if (clazz.isInstance(EmptyConfiguration)) {
            return clazz.cast(EmptyConfiguration)
        }
        if (path.isDirectory()) {
            throw IllegalArgumentException("Path '$path' is a directory")
        }
        val resourceNode =
            if (settings.findResource) {
                val p = settings.basePath.relativize(path)
                val lang = getLangCache(clazz, p.pathString)
                val locale = if (EsuConfig.initialized) EsuConfig.get().locale else Locale.getDefault().language + '_' + Locale.getDefault().country.lowercase()
                val resource = lang.find { it.nameWithoutExtension == locale }
                    ?: lang.firstOrNull { it.nameWithoutExtension.substringBefore('_') == locale.substringBefore('_') }
                resource?.readConfig(clazz, settings)
            } else null
        val loader = createBuilder(resourceNode).let(settings.yamlLoader).path(path).build()
        val node = settings.nodeMapper(configKey, loader.load())
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
                                    return@Processor
                                }
                                val resourceOld = resourceNode?.node("_oc_")?.node(destination.path()) // _oc_ = _old_comments_
                                val allOld = (
                                        if (resourceOld != null && resourceOld.isList)
                                            resourceOld.getList(String::class.java)!!.plus(data.overrideOld)
                                        else
                                            data.overrideOld.toList()
                                        ).map { it.toString().trimIndent() }

                                if (allOld.isEmpty() || destination.comment() == null) {
                                    destination.commentIfAbsent(resourceComment ?: data.value.trimIndent())
                                } else if (data.overrideOld.firstOrNull() == OVERRIDE_ALWAYS) {
                                    destination.comment(resourceComment ?: data.value.trimIndent())
                                } else if (allOld.contains(destination.comment())) {
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
                            val from = parent.node(*name.removePrefix("/").split('.').toTypedArray())
                            if (!from.virtual()) {
                                destination.from(from)
                                from.set(null)
                                from.parent()!!.removeChild(from.key())
                            }
                        }
                    }
                    .addNodeResolver { name, _ ->
                        // Skip kotlin delegate(e.g. lazy) properties
                        if (name.endsWith($$"$delegate")) {
                            NodeResolver.SKIP_FIELD
                        } else null
                    }
                    .build()
                options.mapFactory(MapFactories.insertionOrdered())
                    .serializers {
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
                            .register(CaptionSerializer)
                            .register(ComponentSerializer)
                            .register(DurationSerializer)
                            .register(EnumValueSerializer)
                            .register(JavaDurationSerializer)
                            .register(LocalDateSerializer)
                            .register(LocalTimeSerializer)
                            .register(MessageDataSerializer)
                            .register(PathSerializer)
                            .register(RegexSerializer)
                            .register(TextColorSerializer)
                            .register(
                                { type ->
                                    GenericTypeReflector.erase(type).let { clazz ->
                                        try {
                                            clazz.kotlin.isData
                                        } catch (e: KotlinReflectionNotSupportedError) {
                                            // If we don't have kotlin-reflect
                                            EsuCore.instance.warn(e.toString())
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

    private fun sortPatches(paths: List<Path>): List<Path> {
        data class ResolvedPath(val path: Path) {
            val key: String
            var requires: String?

            init {
                val match = PATCH_FILE_REGEX.matchEntire(path.name)
                if (match != null) {
                    key = match.groupValues[1]
                    requires = match.groupValues[2]
                } else {
                    key = path.name.substringBefore('.')
                    requires = null
                }
            }
        }
        val resolved = LinkedList(paths.map { ResolvedPath(it) })
        val dependencies = mutableMapOf<String, MutableList<ResolvedPath>>()

        for (path in resolved) {
            val requires = path.requires
            if (requires != null) {
                dependencies.getOrPut(requires) { mutableListOf() }.add(path)
            }
        }

        val result = mutableListOf<Path>()

        var lastSize = 0
        while (resolved.isNotEmpty()) {
            if (lastSize == resolved.size) {
                EsuCore.instance.warn("Failed to resolve graph of patch file ${resolved.map { it.path }}, skipping.")
                break
            }
            lastSize = resolved.size
            val iterator = resolved.iterator()
            for (path in iterator) {
                if (path.requires != null) continue
                dependencies[path.key]?.forEach { dep ->
                    dep.requires = null
                }
                result.add(path.path)
                iterator.remove()
            }
        }

        return result
    }

    private fun getLangCache(clazz: Class<*>, path: String): List<LangResource> {
        val classLoader = clazz.classLoader
        val tree = langCache.getIfPresent(classLoader) ?: let {
            val root = TreeNode<List<String>>()
            clazz.jarFile.use { jarFile ->
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
        val nameWithoutExtension = name.substringBefore('.')
        val resourcePath = "lang/$path$name"

        fun save(clazz: Class<*>, path: Path) {
            save(clazz.classLoader, path)
        }

        fun save(classLoader: ClassLoader, path: Path) {
            getConnection(classLoader).getInputStream().use { input ->
                path.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        fun readConfig(clazz: Class<*>, settings: LoaderSettings<*>): ConfigurationNode {
            return readConfig(clazz.classLoader, createBuilder(null).let(settings.yamlLoader))
        }

        fun readConfig(classLoader: ClassLoader, yamlBuilder: YamlConfigurationLoader.Builder): ConfigurationNode {
            val reader = getConnection(classLoader).getInputStream().bufferedReader()
            return yamlBuilder.source { reader }.build().load()
        }

        private fun getConnection(classLoader: ClassLoader): URLConnection {
            val conn = classLoader.getResource(resourcePath)!!.openConnection()
            conn.useCaches = false
            conn.connect()
            return conn
        }
    }

    open class LoaderSettings<T>(
        val yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
        val nodeMapper: (configKey: String, ConfigurationNode) -> ConfigurationNode = { _, node -> node },
        val modifier: (T, Path) -> T = { it, _ -> it },
        val findResource: Boolean = true,
        val basePath: Path = EsuCore.instance.baseConfigPath(),
    ) {

        class Builder<T> {

            var yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it }
            var nodeMapper: (String, ConfigurationNode) -> ConfigurationNode = { _, node -> node }
            var modifier: (T, Path) -> T = { it, _ -> it }
            var findResource: Boolean = true
            var basePath: Path = EsuCore.instance.baseConfigPath()

            fun yamlLoader(yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder): Builder<T> {
                this.yamlLoader = yamlLoader
                return this
            }

            fun nodeMapper(mapper: (String, ConfigurationNode) -> ConfigurationNode): Builder<T> {
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
        val forceLoadConfigs: List<String>? = null,
        val initializeConfigs: List<String>? = null,
        val loadSubDirectories: Boolean = false,
        val configKeyMapper: (Path) -> String = { it.nameWithoutExtension },
        yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
        nodeMapper: (configKey: String, ConfigurationNode) -> ConfigurationNode = { _, node -> node },
        modifier: (T, Path) -> T = { it, _ -> it },
        findResource: Boolean = true,
        basePath: Path = EsuCore.instance.baseConfigPath(),
    ): LoaderSettings<T>(yamlLoader, nodeMapper, modifier, findResource, basePath) {

        constructor(
            vararg forceLoadConfigs: String,
            initializeConfigs: List<String>? = null,
            loadSubDirectories: Boolean = false,
            yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
            keyMapper: (Path) -> String = { it.nameWithoutExtension },
            nodeMapper: (configKey: String, ConfigurationNode) -> ConfigurationNode = { _, node -> node },
            modifier: (T, Path) -> T = { it, _ -> it },
            findResource: Boolean = true,
            basePath: Path = EsuCore.instance.baseConfigPath(),
        ): this(forceLoadConfigs.toList(), initializeConfigs, loadSubDirectories, keyMapper, yamlLoader, nodeMapper, modifier, findResource, basePath)


        class Builder<T> {

            var forceLoadConfigs: List<String>? = null
            var initializeConfigs: List<String>? = null
            var loadSubDirectories: Boolean = false
            var keyMapper: (Path) -> String = { it.nameWithoutExtension }
            var yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it }
            var nodeMapper: (String, ConfigurationNode) -> ConfigurationNode = { _, node -> node }
            var modifier: (T, Path) -> T = { it, _ -> it }
            var findResource: Boolean = true
            var basePath: Path = EsuCore.instance.baseConfigPath()

            fun forceLoadConfigs(vararg forceLoadConfigs: String): Builder<T> {
                this.forceLoadConfigs = forceLoadConfigs.toList()
                return this
            }

            fun forceLoadConfigs(forceLoadConfigs: List<String>?): Builder<T> {
                this.forceLoadConfigs = forceLoadConfigs
                return this
            }

            fun initializeConfigs(initializeConfigs: List<String>?): Builder<T> {
                this.initializeConfigs = initializeConfigs
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

            fun nodeMapper(mapper: (String, ConfigurationNode) -> ConfigurationNode): Builder<T> {
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


            fun build() = LoaderSettingsMulti(forceLoadConfigs, initializeConfigs, loadSubDirectories, keyMapper, yamlLoader, nodeMapper, modifier, findResource, basePath)

        }
    }

}