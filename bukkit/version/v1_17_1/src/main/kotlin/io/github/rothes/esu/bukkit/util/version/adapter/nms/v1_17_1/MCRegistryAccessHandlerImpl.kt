package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_17_1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer

class MCRegistryAccessHandlerImpl: MCRegistryAccessHandler {

    override fun getServerRegistryAccess(): RegistryAccess {
        return MinecraftServer.getServer().registryAccess()
    }

    override fun <T> getRegistryOrThrow(registryAccess: RegistryAccess, registryKey: ResourceKey<out Registry<T>>): Registry<T> {
        return registryAccess.registryOrThrow(registryKey)
    }

    override fun <T> get(registry: Registry<T>, resource: ResourceLocation): T? {
        return registry.get(resource)
    }

    override fun <T: Any> getResourceKey(registry: Registry<T>, item: T): ResourceKey<T> {
        return registry.getResourceKey(item).orElseThrow()
    }

    override fun <T> entrySet(registry: Registry<T>): Set<Map.Entry<ResourceKey<T>, T>> = registry.entrySet()
    override fun <T> values(registry: Registry<T>): Set<T> = registry.toSet()

}