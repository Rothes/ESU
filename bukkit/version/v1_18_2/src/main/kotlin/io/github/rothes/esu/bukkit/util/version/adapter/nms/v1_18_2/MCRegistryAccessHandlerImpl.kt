package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_18_2

import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer

object MCRegistryAccessHandlerImpl: MCRegistryAccessHandler {

    override fun getServerRegistryAccess(): RegistryAccess {
        return MinecraftServer.getServer().registryAccess() // Change: return value is RegistryAccess.Frozen
    }

    override fun <T> getRegistryOrThrow(registryAccess: RegistryAccess, registryKey: ResourceKey<out Registry<T>>): Registry<T> {
        return registryAccess.registryOrThrow(registryKey)
    }

    override fun <T> getNullable(registry: Registry<T>, resource: ResourceLocation): T? {
        return registry.getOptional(resource).orElse(null)
    }

    override fun <T: Any> getResourceKey(registry: Registry<T>, item: T): ResourceKey<T> {
        return registry.getResourceKey(item).orElseThrow()
    }

    override fun <T> entrySet(registry: Registry<T>): Set<Map.Entry<ResourceKey<T>, T>> = registry.entrySet()
    override fun <T> keySet(registry: Registry<T>): Set<ResourceLocation> = registry.keySet()
    override fun <T> values(registry: Registry<T>): Set<T> = registry.toSet()
    override fun <T> size(registry: Registry<T>): Int = registry.size()

}