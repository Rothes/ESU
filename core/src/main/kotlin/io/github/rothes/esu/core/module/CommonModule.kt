package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigLoader.forEachValue
import io.github.rothes.esu.core.configuration.ConfigLoader.map
import io.github.rothes.esu.core.configuration.LoadedConfiguration
import io.github.rothes.esu.core.configuration.MultiConfiguration
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

abstract class CommonModule<C, L> : CommonFeature<C, L>(), Module<C, L> {

    override val name: String = javaClass.simpleName.removeSuffix("Module")

    override val moduleFolder: Path by lazy {
        EsuCore.instance.baseConfigPath().resolve("modules").resolve(name)
    }
    override val configPath: Path by lazy {
        moduleFolder.resolve("config.yml")
    }
    override val langPath: Path by lazy {
        moduleFolder.resolve("lang")
    }

    override fun doReload() {
        ModuleConfigurationsLoader.load(this)
        super.doReload()
    }

    open fun buildConfigLoader(builder: YamlConfigurationLoader.Builder) {
        builder.defaultOptions { options ->
            options.serializers { builder ->
                builder.register(FeatureToggle.DefaultTrue.SERIALIZER).register(FeatureToggle.DefaultFalse.SERIALIZER)
            }
        }
    }

    open fun buildLangLoader(builder: YamlConfigurationLoader.Builder) { }

    open fun preprocessConfig(loadedConfiguration: LoadedConfiguration) {}

    fun User.hasPerm(shortPerm: String): Boolean {
        return hasPermission(perm(shortPerm))
    }

    private object ModuleConfigurationsLoader {

        fun load(root: CommonModule<*, *>) {
            with(root) {
                val configNode = ConfigLoader.loadConfiguration(
                    configPath,
                    ConfigLoader.LoaderSettings(
                        yamlLoader = { buildConfigLoader(it); it },
                    ),
                    configClass,
                )
                val langNodes = ConfigLoader.loadConfigurationMulti(
                    langPath,
                    MultiLangConfiguration::class.java,
                    langClass,
                    ConfigLoader.LoaderSettingsMulti(
                        "en_us",
                        yamlLoader = { buildLangLoader(it); it },
                    )
                )
                preprocessConfig(configNode)
                loadConfig(root, configNode)
                loadLang(root, langNodes)
                configNode.save()
                langNodes.forEachValue { it.save() }
            }
        }

        fun <C, L> loadConfig(feature: Feature<C, L>, node: LoadedConfiguration) {
            feature.setConfigInstance(node.getAs(feature.configClass))

            feature.forEachChild { child, configPath ->
                loadConfig(child, node.node(configPath))
            }
        }

        fun <C, L> loadLang(feature: Feature<C, L>, nodes: MultiConfiguration<LoadedConfiguration>) {
            feature.setLangInstance(nodes.map { node -> node.getAs(feature.langClass) })

            feature.forEachChild { child, configPath ->
                loadLang(child, nodes.map { node -> node.node(configPath) })
            }
        }

        private fun Feature<*, *>.forEachChild(scope: (child: Feature<*, *>, configPath: String) -> Unit) {
            for (child in getFeatures()) {
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
                scope(child, path)
            }
        }
    }

}