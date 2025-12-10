package io.github.rothes.esu.bukkit.util.version.adapter.nms.v19_3

import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer

object MCRegistryAccessHandlerImpl: MCRegistryAccessHandler {

    override fun getServerRegistryAccess(): RegistryAccess {
        return MinecraftServer.getServer().registryAccess()
    }

    override fun <T: Any> getRegistryOrThrow(registryAccess: RegistryAccess, registryKey: ResourceKey<out Registry<T>>): Registry<T> {
        return registryAccess.registryOrThrow(registryKey)
    }

    // Change: Registry is now interface

    override fun <T: Any> getNullable(registry: Registry<T>, key: ResourceKey<T>): T? {
        return registry.getOptional(key).orElse(null)
    }

    override fun <T: Any> getResourceKey(registry: Registry<T>, item: T): ResourceKey<T> {
        return registry.getResourceKey(item).orElseThrow()
    }

    override fun <T: Any> getId(registry: Registry<T>, item: T): Int {
        return registry.getId(item)
    }

    override fun <T: Any> entrySet(registry: Registry<T>): Set<Map.Entry<ResourceKey<T>, T>> = registry.entrySet()
    override fun <T: Any> keySet(registry: Registry<T>): Set<ResourceKey<T>> = registry.registryKeySet()
    override fun <T: Any> values(registry: Registry<T>): Set<T> = registry.toSet()
    override fun <T: Any> size(registry: Registry<T>): Int = registry.size()

}