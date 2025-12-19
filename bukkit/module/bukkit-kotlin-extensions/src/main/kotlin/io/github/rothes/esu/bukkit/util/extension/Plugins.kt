package io.github.rothes.esu.bukkit.util.extension

import org.bukkit.plugin.Plugin
import java.lang.reflect.Proxy

fun Plugin.createChild(name: String = this.name, forceEnabled: Boolean = false): Plugin {
    return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Plugin::class.java)) { p, method, args ->
        when (method.name) {
            "isEnabled" -> forceEnabled || this.isEnabled
            "getName" -> name
            "hashCode" -> name.hashCode()
            "equals" -> p === args[0] || (args[0] is Plugin && name == (args[0] as Plugin).name)
            else -> {
                if (args == null)
                    method.invoke(this)
                else
                    method.invoke(this, args)
            }
        }
    } as Plugin
}