package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey

interface MCRegistryAccessHandler {

    fun getServerRegistryAccess(): RegistryAccess

    fun <T: Any> getRegistryOrThrow(registryAccess: RegistryAccess, registryKey: ResourceKey<out Registry<T>>): Registry<T>

    fun <T: Any> getNullable(registry: Registry<T>, key: ResourceKey<T>): T?

    fun <T: Any> getResourceKey(registry: Registry<T>, item: T): ResourceKey<T>
    fun <T: Any> getId(registry: Registry<T>, item: T): Int

    fun <T: Any> entrySet(registry: Registry<T>): Set<Map.Entry<ResourceKey<T>, T>>
    fun <T: Any> keySet(registry: Registry<T>): Set<ResourceKey<T>>
    fun <T: Any> values(registry: Registry<T>): Set<T>
    fun <T: Any> size(registry: Registry<T>): Int

}