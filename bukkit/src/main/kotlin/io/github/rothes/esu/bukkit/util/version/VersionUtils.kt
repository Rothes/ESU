package io.github.rothes.esu.bukkit.util.version

object VersionUtils {

    fun <T> Class<T>.versioned(): T {
        return Versioned<Any, T>(this).handle
    }

}