package io.github.rothes.esu.core.configuration

import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.lib.configurate.ConfigurationNode
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

data class LoadedConfiguration(
    val context: LoaderContext,
    val path: Path,
    val node: ConfigurationNode,
    val resourceNode: ConfigurationNode?,
) {

    fun save() {
        var root = node
        while (true) {
            val parent = node.parent() ?: break
            root = parent
        }
        context.loader.save(root)
    }

    fun <T> getAs(clazz: Class<T>): T {
        if (clazz.isInstance(EmptyConfiguration)) return clazz.cast(EmptyConfiguration)
        else if (clazz.isInstance(Unit)) return clazz.cast(Unit)

        if (context.mergeResources && resourceNode != null) {
            val defaultNode = context.loader.createNode()
            defaultNode.set(clazz.getConstructor().newInstance())
            mergeLang(defaultNode, resourceNode, node)
        }
        val instance = node.require(clazz)
        node.set(instance)
        return instance
    }

    fun node(vararg path: Any): LoadedConfiguration {
        return copy(
            node = this.node.node(*path),
            resourceNode = this.resourceNode?.node(*path),
        )
    }

    private fun mergeLang(def: ConfigurationNode, from: ConfigurationNode, to: ConfigurationNode) {
        if (def.isMap) {
            for (path in def.childrenMap().keys) {
                mergeLang(def.node(path), from.node(path), to.node(path))
            }
        } else {
            if (!from.virtual() && (to.virtual() || to.raw() == def.raw())) {
                to.raw(from.raw())
            }
        }
    }

    data class LoaderContext(
        val loader: YamlConfigurationLoader,
        val mergeResources: Boolean,
    )

}