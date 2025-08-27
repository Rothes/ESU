package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore

object ModuleManager {

    private val modules = LinkedHashMap<String, Module<*, *>>()

    fun registeredModules() = modules.values

    operator fun get(moduleName: String) = modules[moduleName]

    fun addModule(module: Module<*, *>) {
        removeModule(module.name)
        modules.putIfAbsent(module.name, module)

        reloadModule(module)
        try {
            if (module.canUse()) {
                enableModule(module)
            }
        } catch(e: Throwable) {
            EsuCore.instance.err("Failed to check&enable module ${module.name}", e)
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

    fun reloadModules() {
        for (module in modules.values) {
            removeModule(module.name)
        }
    }

    private fun reloadModule(module: Module<*, *>) {
        try {
            module.reloadConfig()
        } catch (e: Throwable) {
            EsuCore.instance.err("Failed to read config of module ${module.name}", e)
        }
    }

}