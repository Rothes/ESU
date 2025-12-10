package io.github.rothes.esu.bukkit.util.version.adapter.nms.v19_3

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

object ResourceKeyHandlerImpl: ResourceKeyHandler {

    override fun <T: Any> parseResourceKey(registry: Registry<T>, id: String): ResourceKey<T> {
        val value = ResourceLocation.tryParse(id) ?: throw ResourceKeyHandler.BadIdentifierException(id)
        return ResourceKey.create(registry.key(), value)
    }

    override fun getResourceKeyString(resourceKey: ResourceKey<*>): String {
        val location = resourceKey.location()
        return if ((location.namespace == ResourceLocation.DEFAULT_NAMESPACE)) location.path
        else location.toString()
    }

}