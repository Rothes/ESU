package io.github.rothes.esu.bukkit.util.version.adapter.nms.v21_11

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

object ResourceKeyHandlerImpl: ResourceKeyHandler {

    override fun <T: Any> parseResourceKey(registry: Registry<T>, id: String): ResourceKey<T> {
        val location = Identifier.tryParse(id) ?: throw ResourceKeyHandler.BadIdentifierException(id)
        return ResourceKey.create(registry.key(), location)
    }

    override fun getResourceKeyString(resourceKey: ResourceKey<*>): String {
        val id = resourceKey.identifier()
        return if ((id.namespace == Identifier.DEFAULT_NAMESPACE)) id.path
        else id.toString()
    }

}