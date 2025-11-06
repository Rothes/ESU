package io.github.rothes.esu.bukkit.configuration

import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.leangen.geantyref.TypeToken
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import java.lang.reflect.Type
import java.util.function.Predicate

class RegistryValueSerializer<T: Any>(
    val accessHandler: MCRegistryAccessHandler,
    registryKey: ResourceKey<out Registry<T>>,
    type: TypeToken<T>,
): ScalarSerializer<T>(type) {

    constructor(accessHandler: MCRegistryAccessHandler, registryKey: ResourceKey<out Registry<T>>, clazz: Class<T>): this(accessHandler, registryKey, TypeToken.get(clazz))

    val registry = accessHandler.getRegistryOrThrow(accessHandler.getServerRegistryAccess(), registryKey)

    override fun deserialize(type: Type?, obj: Any?): T? {
        val key = ResourceLocation.tryParse(obj.toString().lowercase()) ?: let {
            IllegalArgumentException("Failed to parse $obj to ResourceLocation, ignored.").printStackTrace()
            return null
        }
        return accessHandler.getNullable(registry, key)
    }

    override fun serialize(item: T, typeSupported: Predicate<Class<*>?>?): Any? {
        val key = accessHandler.getResourceKey(registry, item)

        return if ((key.location().namespace == ResourceLocation.DEFAULT_NAMESPACE)) key.location().path
        else key.location().toString()
    }

}