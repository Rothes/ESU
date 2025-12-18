package io.github.rothes.esu.bukkit.configuration

import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.leangen.geantyref.TypeToken
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import java.lang.reflect.Type
import java.util.function.Predicate

class RegistryValueSerializer<T: Any>(
    val accessHandler: MCRegistryAccessHandler,
    registryKey: ResourceKey<out Registry<T>>,
    type: TypeToken<T>,
): ScalarSerializer<T>(type) {

    companion object {
        private val KEY_HANDLER by Versioned(ResourceKeyHandler::class.java)
    }

    constructor(accessHandler: MCRegistryAccessHandler, registryKey: ResourceKey<out Registry<T>>, clazz: Class<T>): this(accessHandler, registryKey, TypeToken.get(clazz))

    val registry = accessHandler.getRegistryOrThrow(accessHandler.getServerRegistryAccess(), registryKey)

    override fun deserialize(type: Type?, obj: Any?): T? {
        val key = try {
            KEY_HANDLER.parseResourceKey(registry, obj.toString().lowercase())
        } catch (e: ResourceKeyHandler.BadIdentifierException) {
            e.printStackTrace()
            return null
        }
        return accessHandler.getNullable(registry, key) ?: let {
            IllegalArgumentException("Key $obj is not in the registry $registry, ignored.").printStackTrace()
            null
        }
    }

    override fun serialize(item: T, typeSupported: Predicate<Class<*>?>?): Any {
        val key = accessHandler.getResourceKey(registry, item)
        return KEY_HANDLER.getResourceKeyString(key)
    }

}