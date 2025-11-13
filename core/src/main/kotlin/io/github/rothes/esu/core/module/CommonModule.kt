package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.module.configuration.FeatureNodeMapper
import io.github.rothes.esu.core.module.configuration.FeatureNodeMapper.Companion.nodeMapper
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
        ConfigLoader.load(
            configPath,
            configClass,
            ConfigLoader.LoaderSettings(
                yamlLoader = { buildConfigLoader(it); it },
                nodeMapper = nodeMapper(FeatureNodeMapper.TargetClass.CONFIG)
            )
        )
        fun clearLang(feature: Feature<*, *>) {
            val map = feature.lang.configs as MutableMap
            map.clear()
            for (feature in feature.getFeatures()) {
                clearLang(feature)
            }
        }
        clearLang(this)
        ConfigLoader.loadMulti(
            langPath,
            langClass,
            ConfigLoader.LoaderSettingsMulti(
                "en_us",
                yamlLoader = { buildLangLoader(it); it },
                nodeMapper = nodeMapper(FeatureNodeMapper.TargetClass.LANG)
            )
        )
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

    fun User.hasPerm(shortPerm: String): Boolean {
        return hasPermission(perm(shortPerm))
    }

}