package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.core.configuration.serializer.ComponentSerializer
import io.github.rothes.esu.core.configuration.serializer.EnumValueSerializer
import io.github.rothes.esu.core.configuration.serializer.MapSerializer
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
import java.nio.file.Path
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension

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
        val loader = builder.invoke(
            YamlConfigurationLoader.builder()
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .headerMode(HeaderMode.PRESERVE)
                .path(path)
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
                                .register(EnumValueSerializer)
                                .register(ComponentSerializer)
                                .register(TextColorSerializer)
                        }
                }
        ).build()
        val node = loader.load() ?: CommentedConfigurationNode.root(loader.defaultOptions())
        val t = modifier.invoke(node.require(clazz))
        node.set(clazz, t)
        loader.save(node)
        return t
    }

}