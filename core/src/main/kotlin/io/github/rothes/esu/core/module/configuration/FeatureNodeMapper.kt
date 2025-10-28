package io.github.rothes.esu.core.module.configuration

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.lib.configurate.ConfigurationNode

class FeatureNodeMapper(
    val root: Feature<*, *>,
    val targetClass: TargetClass
) {

    val nodeMapper: (String, ConfigurationNode) -> ConfigurationNode = { key, node ->
        load(root, node, key)
        node
    }

    private fun <C, L> load(feature: Feature<C, L>, node: ConfigurationNode, key: String) where C : ConfigurationPart, L : ConfigurationPart {
        when (targetClass) {
            TargetClass.CONFIG -> {
                val instance = node.require(feature.configClass)
                node.set(instance)
                feature.setConfigInstance(instance)
            }
            TargetClass.LANG -> {
                val instance = node.require(feature.langClass)
                node.set(instance)
                val map = feature.lang.configs as MutableMap
                map[key] = instance
            }
        }
        for (child in feature.getFeatures()) {
            val name = child.name
            val path = buildString(name.length + 4) {
                append(name.firstOrNull()?.lowercaseChar() ?: return@buildString)
                var i = 1
                while (i < name.length) {
                    if (name[i].isUpperCase()) {
                        append('-')
                        append(name[i].lowercaseChar())
                    } else {
                        append(name[i])
                    }
                    i++
                }
            }
            load(child, node.node(path), key)
        }
    }

    enum class TargetClass {
        CONFIG,
        LANG,
    }

    companion object {
        fun Feature<*, *>.nodeMapper(targetClass: TargetClass) = FeatureNodeMapper(this, targetClass).nodeMapper
    }

}