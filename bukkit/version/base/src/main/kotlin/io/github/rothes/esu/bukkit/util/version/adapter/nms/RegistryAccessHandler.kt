package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

interface RegistryAccessHandler {

    fun getServerRegistryAccess(): RegistryAccess

    fun <T> getRegistryOrThrow(registryAccess: RegistryAccess, registryKey: ResourceKey<out Registry<T>>): Registry<T>

    fun <T> get(registry: Registry<T>, resource: ResourceLocation): T?

    fun <T: Any> getResourceKey(registry: Registry<T>, item: T): ResourceKey<T>

    fun <T> entrySet(registry: Registry<T>): Set<Map.Entry<ResourceKey<T>, T>>
    fun <T> values(registry: Registry<T>): Set<T>

}