package io.github.rothes.esu.bukkit.configuration

import io.github.rothes.esu.bukkit.util.version.adapter.nms.RegistryAccessHandler
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.leangen.geantyref.TypeToken
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import java.lang.reflect.Type
import java.util.function.Predicate

class RegistryValueSerializer<T: Any>(
    val accessHandler: RegistryAccessHandler,
    val registryKey: ResourceKey<out Registry<T>>,
    clazz: TypeToken<T>,
    registryAccess: RegistryAccess = accessHandler.getServerRegistryAccess(),
): ScalarSerializer<T>(clazz) {

    constructor(
        accessHandler: RegistryAccessHandler,
        registryKey: ResourceKey<out Registry<T>>,
        clazz: Class<T>,
        registryAccess: RegistryAccess = accessHandler.getServerRegistryAccess(),
    ): this(accessHandler, registryKey, TypeToken.get(clazz), registryAccess)

    val registry = accessHandler.getRegistryOrThrow(registryAccess, registryKey)

    override fun deserialize(type: Type?, obj: Any?): T? {
        val key = ResourceLocation.tryParse(obj.toString().lowercase()) ?: let {
            IllegalArgumentException("Failed to parse $obj to ResourceLocation, ignored.").printStackTrace()
            return null
        }
        ResourceKey.create(this.registryKey, key)
        return accessHandler.get(registry, key)
    }

    override fun serialize(item: T, typeSupported: Predicate<Class<*>?>?): Any? {
        val key = accessHandler.getResourceKey(registry, item)

        return if ((key.location().namespace == ResourceLocation.DEFAULT_NAMESPACE)) key.location().path
        else key.location().toString()
    }

}