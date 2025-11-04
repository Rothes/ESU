package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_21_3

import io.github.rothes.esu.bukkit.util.version.adapter.nms.RegistryAccessHandler
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer

class RegistryAccessHandlerImpl: RegistryAccessHandler {

    override fun getServerRegistryAccess(): RegistryAccess {
        return MinecraftServer.getServer().registryAccess()
    }

    override fun <T> getRegistryOrThrow(registryAccess: RegistryAccess, registryKey: ResourceKey<out Registry<T>>): Registry<T> {
        return registryAccess.lookupOrThrow(registryKey) // Change: method name
    }

    override fun <T> get(registry: Registry<T>, resource: ResourceLocation): T? {
        return registry.getValue(resource)
    }

    override fun <T: Any> getResourceKey(registry: Registry<T>, item: T): ResourceKey<T> {
        return registry.getResourceKey(item).orElseThrow()
    }

    override fun <T> entrySet(registry: Registry<T>): Set<Map.Entry<ResourceKey<T>, T>> = registry.entrySet()
    override fun <T> values(registry: Registry<T>): Set<T> = registry.toSet()

}