package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.v18

import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.PlayerEntityVisibilityHandler
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import io.github.rothes.esu.core.util.UnsafeUtils.usNullableObjAccessor
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.ref.WeakReference
import java.util.*

object PlayerEntityVisibilityHandlerImpl : PlayerEntityVisibilityHandler {

    private val hiddenEntities = CraftPlayer::class.java.getDeclaredField("hiddenEntities").usNullableObjAccessor
    private val pluginWeakReferences = CraftPlayer::class.java.getDeclaredMethod("getPluginWeakReference", Plugin::class.java).handle
    private val entityHandleGetter by Versioned(EntityHandleGetter::class.java)

    override fun forceShowEntity(player: Player, bukkitEntity: Entity, plugin: Plugin) {
        @Suppress("UNCHECKED_CAST")
        val map = hiddenEntities[player] as MutableMap<UUID, MutableSet<WeakReference<Plugin>>>
        val uuid = entityHandleGetter.getHandle(bukkitEntity).uuid
        val set = map[uuid] ?: return
        val pluginReference = pluginWeakReferences.invokeExact(plugin) as WeakReference<*>
        if (!set.remove(pluginReference)) return
        if (set.isEmpty()) map.remove(uuid)
    }

}