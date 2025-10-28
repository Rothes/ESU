package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.EnableTogglable
import io.github.rothes.esu.core.user.User

interface Feature<C: ConfigurationPart, L: ConfigurationPart> {

    val name: String
    val enabled: Boolean
    val parent: Feature<*, *>?
    val module: Module<*, *>

    val configClass: Class<C>
    val langClass: Class<L>

    val config: C
    val lang: MultiLangConfiguration<L>

    fun setConfigInstance(instance: C) {}
    fun setEnabled(value: Boolean) {}
    fun setParent(parent: Feature<*, *>?) {}

    fun toggleByAvailable(): AvailableCheck {
        val available = isAvailable()

        var changed = false
        if (available.value && !enabled) {
            onEnable()
            setEnabled(true)
            changed = true
        } else if (!available.value && enabled) {
            setEnabled(false)
            onDisable()
            changed = true
        }

        if (changed) {
            // Notify children
            for (feature in getFeatures()) {
                feature.toggleByAvailable()
            }
        }

        return available
    }
    fun getFeatureMap(): Map<String, Feature<*, *>>
    fun getFeatures(): List<Feature<*, *>>
    fun getFeature(name: String): Feature<*, *>?
    fun registerFeature(child: Feature<*, *>)

    fun isAvailable(): AvailableCheck {
        val config = config
        if (config is EnableTogglable && !config.enabled)
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

    fun onTerminate() {
        getFeatures().forEach { it.onTerminate() }
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