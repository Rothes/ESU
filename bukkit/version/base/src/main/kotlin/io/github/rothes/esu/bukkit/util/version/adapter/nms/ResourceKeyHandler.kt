package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

interface ResourceKeyHandler {

    fun <T: Any> parseResourceKey(registry: Registry<T>, id: String): ResourceKey<T>
    fun getResourceKeyString(resourceKey: ResourceKey<*>): String

    class BadIdentifierException(val id: String): RuntimeException("Failed to parse identifier: $id")

}