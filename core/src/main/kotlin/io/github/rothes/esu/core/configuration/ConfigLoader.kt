package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.EsuConfig
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.serializer.ComponentSerializer
import io.github.rothes.esu.core.configuration.serializer.DurationSerializer
import io.github.rothes.esu.core.configuration.serializer.EnumValueSerializer
import io.github.rothes.esu.core.configuration.serializer.JavaDurationSerializer
import io.github.rothes.esu.core.configuration.serializer.LocalDateSerializer
import io.github.rothes.esu.core.configuration.serializer.LocalTimeSerializer
import io.github.rothes.esu.core.configuration.serializer.MapSerializer
import io.github.rothes.esu.core.configuration.serializer.OptionalSerializer
import io.github.rothes.esu.core.configuration.serializer.PathSerializer
import io.github.rothes.esu.core.configuration.serializer.TextColorSerializer
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.loader.HeaderMode
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.MapFactories
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists
import kotlin.jvm.optionals.getOrNull

object ConfigLoader {

    inline fun <reified T: MultiConfiguration<D>, reified D: ConfigurationPart>
            loadMulti(path: Path, vararg forceLoad: String,
                      builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
                      modifier: (D) -> D = { it }): T {
        return loadMulti(path, D::class.java, forceLoad = forceLoad, builder, modifier)
    }

    inline fun <reified T: MultiConfiguration<D>, D: ConfigurationPart>
            loadMulti(path: Path, clazz: Class<D>, vararg forceLoad: String,
                      builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
                      modifier: (D) -> D = { it }): T {
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
                EsuCore.instance.info("Created symbolic link: [$source] -> [$path]")
            }
        }
        return T::class.java.getConstructor(Map::class.java).newInstance(
            buildMap {
                if (path.isDirectory()) {
                    path.forEachDirectoryEntry { file ->
                        put(file.nameWithoutExtension, load(file, clazz, builder, modifier))
                    }
                }
                forceLoad.filter { !containsKey(it) }.map { path.resolve(it) }.forEach { file ->
                    put(file.nameWithoutExtension, load(file, clazz, builder, modifier))
                }
            }
        )
    }

    inline fun <reified T> load(path: Path,
                                builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
                                modifier: (T) -> T = { it }): T {
        return load(path, T::class.java, builder, modifier)
    }

    inline fun <T> load(path: Path, clazz: Class<T>,
                        builder: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder = { it },
                        modifier: (T) -> T = { it }): T {
        if (clazz.isInstance(EmptyConfiguration)) {
            return clazz.cast(EmptyConfiguration)
        }
        if (path.isDirectory()) {
            throw IllegalArgumentException("Path '$path' is a directory")
        }
        val loader = createBuilder().path(path).let(builder).build()
        val node = loader.load() ?: CommentedConfigurationNode.root(loader.defaultOptions())
        val t = modifier.invoke(node.require(clazz))
        node.set(clazz, t)
        loader.save(node)
        return t
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
                val factory: ObjectMapper.Factory = ObjectMapper.factoryBuilder().build()
                options.mapFactory(MapFactories.insertionOrdered())
                    .serializers {
                        it.register(
                            { type ->
                                GenericTypeReflector.erase(type).let { clazz ->
                                    ConfigurationPart::class.java.isAssignableFrom(clazz)
//                                                    || try {
//                                                        clazz.kotlin.isData
//                                                    } catch (e: Throwable) {
//                                                        false
//                                                    }
                                }
                            },
                            factory.asTypeSerializer())
                            .registerAnnotatedObjects(factory)
                            .register(TypeToken.get(Map::class.java), MapSerializer)
                            .register(TypeToken.get(Optional::class.java), OptionalSerializer)
                            .register(ComponentSerializer)
                            .register(DurationSerializer)
                            .register(EnumValueSerializer)
                            .register(JavaDurationSerializer)
                            .register(LocalDateSerializer)
                            .register(LocalTimeSerializer)
                            .register(PathSerializer)
                            .register(TextColorSerializer)
                    }
            }
    }

}