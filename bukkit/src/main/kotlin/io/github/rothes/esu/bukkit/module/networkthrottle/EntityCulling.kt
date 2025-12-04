package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.CullDataManager
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.RaytraceHandler
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryValueSerializers
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.util.extension.ClassUtils
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent

object EntityCulling : CommonFeature<EntityCulling.FeatureConfig, EmptyConfiguration>() {

    private val raytraceHandler =
        if (MCRegistryValueSerializers.isSupported && ServerCompatibility.serverVersion >= 18)
            RaytraceHandler::class.java.versioned().also {
                registerFeature(it)
                CullDataManager.raytraceHandler = it
            }
        else null

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents() ?: let {
            if (ServerCompatibility.serverVersion < 18) {
                plugin.err("[EntityCulling] This feature requires Spigot 1.18 .")
                return Feature.AvailableCheck.fail { "Server is not supported".message }
            }
            raytraceHandler?.checkConfig()
        }
    }

    override fun onEnable() {
        Listeners.register()
        if (EntityRemoveListeners.isSupported)
            EntityRemoveListeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
        if (EntityRemoveListeners.isSupported)
            EntityRemoveListeners.unregister()
        CullDataManager.shutdown()
    }

    private fun broadcastRemoved(entity: Entity) {
        raytraceHandler!!.onEntityRemove(entity)
    }

    private object Listeners: Listener {

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            CullDataManager.remove(event.player)
            broadcastRemoved(event.player)
        }

        @EventHandler
        fun onChangeWorld(event: PlayerChangedWorldEvent) {
            // Release memory
            CullDataManager[event.player].showAll()
        }

        @EventHandler
        fun onTeleport(event: PlayerTeleportEvent) {
            // Release memory
            CullDataManager[event.player].showAll()
        }

    }

    private object EntityRemoveListeners: Listener {

        val isSupported = ClassUtils.existsClass("org.bukkit.event.entity.EntityRemoveEvent")

        @EventHandler
        fun onEntityRemove(event: EntityRemoveEvent) {
            broadcastRemoved(event.entity)
        }

    }

    @Comment("""
        Smart Occlusion Culling to save upload bandwidth.
        Plugin will hide invisible entities to players.
    """)
    class FeatureConfig(): BaseFeatureConfiguration()

}