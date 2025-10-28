package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore

object ModuleManager {

    private val modules = LinkedHashMap<String, Module<*, *>>()

    fun registeredModules(): List<Module<*, *>> = modules.values.toList()

    operator fun get(moduleName: String) = modules[moduleName]

    fun addModule(module: Module<*, *>) {
        synchronized(modules) {
            removeModule(module.name)
            modules.putIfAbsent(module.name, module)
        }

        reloadModule(module)
    }

    fun removeModule(module: Module<*, *>) {
        removeModule(module.name)
    }

    fun removeModule(name: String) {
        val removed = synchronized(modules) {
            modules.remove(name)
        }
        removed?.let { module ->
            if (module.enabled) {
                forceDisableModule(module)
            }
            module.onTerminate()
        }
    }

    fun forceEnableModule(module: Module<*, *>): Boolean {
        try {
            module.onEnable()
            module.setEnabled(true)
            return true
        } catch (e: Throwable) {
            EsuCore.instance.err("Failed to enable module ${module.name}", e)
            return false
        }
    }

    fun forceDisableModule(module: Module<*, *>): Boolean {
        try {
            module.setEnabled(false)
            module.onDisable()
            return true
        } catch (e: Throwable) {
            EsuCore.instance.err("Failed to disable module ${module.name}", e)
            return false
        }
    }

    fun reloadModules() {
        for (module in modules.values) {
            reloadModule(module)
        }
    }

    private fun reloadModule(module: Module<*, *>) {
        try {
            module.doReload()
        } catch (e: Throwable) {
            EsuCore.instance.err("Failed to read config of module ${module.name}", e)
        }
    }

}