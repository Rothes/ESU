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
        if (context.mergeResources && resourceNode != null) {
            node.mergeFrom(resourceNode)
        }
        if (clazz.isInstance(EmptyConfiguration)) return clazz.cast(EmptyConfiguration)
        else if (clazz.isInstance(Unit)) return clazz.cast(Unit)

        val instance = node.require(clazz)
        node.set(instance)
        return instance
    }

    fun node(vararg path: Any): LoadedConfiguration {
        return LoadedConfiguration(
            this.context,
            this.path,
            this.node.node(*path),
            this.resourceNode?.node(*path),
        )
    }

    data class LoaderContext(
        val loader: YamlConfigurationLoader,
        val mergeResources: Boolean,
    )

}