package io.github.rothes.esu.bukkit.util.extension

import org.bukkit.plugin.Plugin
import java.lang.reflect.Proxy

fun Plugin.createChild(name: String = this.name, forceEnabled: Boolean = false): Plugin {
    return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Plugin::class.java)) { _, method, args ->
        when (method.name) {
            "isEnabled" -> forceEnabled || this.isEnabled
            "getName" -> name
            "hashCode" -> this.hashCode() // Not necessary but we want to avoid cost of method#invoke
            else -> {
                if (args == null)
                    method.invoke(this)
                else
                    method.invoke(this, args)
            }
        }
    } as Plugin
}