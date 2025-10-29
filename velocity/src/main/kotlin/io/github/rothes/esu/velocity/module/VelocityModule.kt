package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.velocity.plugin

abstract class VelocityModule<C: ConfigurationPart, L: ConfigurationPart> : CommonModule<C, L>() {

    protected val registeredListeners = arrayListOf<Pair<Any, Any>>()

    override fun onDisable() {
        super.onDisable()
        for (listener in registeredListeners) {
            unregisterListener(listener.first, listener.second)
        }
        registeredListeners.clear()
    }

    fun registerListener(listener: Any, pluginInstance: Any = plugin.bootstrap) {
        plugin.server.eventManager.register(pluginInstance, listener)
        registeredListeners.add(listener to pluginInstance)
    }

    fun unregisterListener(listener: Any, pluginInstance: Any = plugin.bootstrap) {
        if (pluginInstance === plugin.bootstrap) {
            if (plugin.enabled) {
                plugin.server.eventManager.unregisterListener(pluginInstance, listener)
            }
        } else {
            plugin.server.eventManager.unregisterListener(pluginInstance, listener)
        }
    }

}