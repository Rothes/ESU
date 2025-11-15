package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.EnableTogglable
import io.github.rothes.esu.core.user.User

interface Feature<C, L> {

    val name: String
    val enabled: Boolean
    val parent: Feature<*, *>?
    val module: Module<*, *>

    val configClass: Class<C>
    val langClass: Class<L>

    val config: C
    val lang: MultiLangConfiguration<L>

    val permissionNode: String

    fun setConfigInstance(instance: C) {}
    fun setEnabled(value: Boolean) {
        // Notify children
        for (feature in getFeatures().let { if (enabled) it else it.reversed() }) {
            feature.toggleByAvailable()
        }
    }
    fun setParent(parent: Feature<*, *>?) {}

    fun toggleByAvailable(): AvailableCheck {
        val available = isAvailable()

        if (available.value && !enabled) {
            enableInternal()
        } else if (!available.value && enabled) {
            disableInternal()
        }

        return available
    }

    fun getFeatureMap(): Map<String, Feature<*, *>>
    fun getFeatures(): List<Feature<*, *>>
    fun getFeature(name: String): Feature<*, *>?
    fun registerFeature(child: Feature<*, *>)

    fun isAvailable(): AvailableCheck {
        return checkUnavailable() ?: AvailableCheck.OK
    }
    fun checkUnavailable(): AvailableCheck? {
        val config = config
        if (config is EnableTogglable && !config.enabled)
            return AvailableCheck.fail { "Feature not enabled".message }
        var parent = parent
        while (parent != null) {
            if (!parent.enabled) {
                return AvailableCheck.fail { "Parent ${parent!!.name} is not enabled".message }
            }
            parent = parent.parent
        }
        return null
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

    fun perm(shortPerm: String): String = "$permissionNode.${shortPerm.lowercase()}"

    class AvailableCheck(
        val value: Boolean,
        val messageBuilder: ((User) -> MessageData)?
    ) {

        companion object {
            val OK = AvailableCheck(true, null)
            fun fail(messageBuilder: (User) -> MessageData) = AvailableCheck(false, messageBuilder)
        }
    }

    private companion object {

        private fun Feature<*, *>.enableInternal() {
            try {
                onEnable()
                setEnabled(true)
            } catch (e: Throwable) {
                EsuBootstrap.instance.err("An exception occurred while enabling $name", e)
                disableInternal()
            }
        }

        private fun Feature<*, *>.disableInternal() {
            try {
                setEnabled(false)
                onDisable()
            } catch (e: Throwable) {
                EsuBootstrap.instance.err("An exception occurred while disabling $name", e)
            }
        }

    }

}