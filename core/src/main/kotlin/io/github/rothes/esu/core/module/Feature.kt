package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User

interface Feature<C: ConfigurationPart, L: ConfigurationPart> {

    val name: String
    val enabled: Boolean
    val parent: Feature<*, *>?
    val module: Module<*, *>

    val configClass: Class<C>
    val langClass: Class<L>

    val config: C
    val lang: MultiLocaleConfiguration<L>

    fun setConfigInstance(instance: C) {}
    fun setEnabled(value: Boolean) {
        if (!value) {
            for (feature in getFeatures()) {
                if (feature.enabled) {
                    feature.setEnabled(false)
                    feature.onDisable()
                }
            }
        }
    }
    fun setParent(parent: Feature<*, *>?) {}

    fun toggleByAvailable(): AvailableCheck {
        val available = isAvailable()

        if (available.value && !enabled) {
            onEnable()
            setEnabled(true)
            for (feature in getFeatures()) {
                feature.toggleByAvailable()
            }
        } else if (!available.value && enabled) {
            setEnabled(false)
            onDisable()
        }

        return available
    }
    fun getFeatureMap(): Map<String, Feature<*, *>>
    fun getFeatures(): List<Feature<*, *>>
    fun getFeature(name: String): Feature<*, *>?
    fun registerFeature(child: Feature<*, *>)

    fun isAvailable(): AvailableCheck {
        val config = config
        if (config is BaseModuleConfiguration && !config.moduleEnabled)
            return AvailableCheck(false) { "Module not enabled".message }

        if (config is BaseFeatureConfiguration && !config.enabled)
            return AvailableCheck(false) { "Feature not enabled".message }

        var parent = parent
        while (parent != null) {
            if (!parent.enabled) {
                return AvailableCheck.fail { "Parent ${parent!!.name} is not enabled".message }
            }
            parent = parent.parent
        }

        return AvailableCheck.OK
    }
    fun onEnable()
    fun onDisable() {}
    fun onReload() {
        for (feature in getFeatures()) {
            feature.onReload()
        }
        toggleByAvailable()
    }

    class AvailableCheck(
        val value: Boolean,
        val messageBuilder: ((User) -> MessageData)?
    ) {

        companion object {
            val OK = AvailableCheck(true, null)
            fun fail(messageBuilder: (User) -> MessageData) = AvailableCheck(false, messageBuilder)
        }
    }

}