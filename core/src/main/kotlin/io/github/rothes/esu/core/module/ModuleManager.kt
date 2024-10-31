package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore

object ModuleManager {

    private val modules = HashMap<String, Module<*, *>>()

    fun registeredModules() = modules.values

    operator fun get(moduleName: String) = modules[moduleName]

    fun addModule(module: Module<*, *>) {
        removeModule(module.name)
        modules.putIfAbsent(module.name, module)

        try {
            module.reloadConfig()
        } catch (e: Throwable) {
            EsuCore.instance.err("Failed to read config of module ${module.name}", e)
        }
        if (module.canUse()) {
            enableModule(module)
        }
    }

    fun enableModule(module: Module<*, *>): Boolean {
        try {
            module.enable()
            module.enabled = true
            return true
        } catch (e: Throwable) {
            EsuCore.instance.err("Failed to enable module ${module.name}", e)
            return false
        }
    }

    fun removeModule(module: Module<*, *>) {
        removeModule(module.name)
    }

    fun removeModule(name: String) {
        modules.remove(name)?.let { module ->
            if (module.enabled) {
                disableModule(module)
            }
        }
    }

    fun disableModule(module: Module<*, *>): Boolean {
        try {
            module.enabled = false
            module.disable()
            return true
        } catch (e: Throwable) {
            EsuCore.instance.err("Failed to disable module ${module.name}", e)
            return false
        }
    }

}