package io.github.rothes.esu.bukkit.module.anticheat

import io.github.rothes.esu.bukkit.module.anticheat.NoClientTeleportConfirm.Accessors.awaitingPositionFromClient
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityLevelGetter
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.phys.Vec3
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

object NoClientTeleportConfirm : CommonFeature<NoClientTeleportConfirm.FeatureConfig, Unit>() {

    override fun onEnable() {
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
    }

    private object Listeners : Listener {

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onInteract(event: PlayerInteractEvent) {
            // Server software actually detects awaitingPositionFromClient != null here
            // But I'm not sure if that applies to legacy Minecraft too.
            checkInteractEvent(event, null)
        }
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onInteract(event: PlayerInteractEntityEvent) {
            checkInteractEvent(event, event.rightClicked)
        }
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onInteract(event: EntityDamageByEntityEvent) {
            val damager = event.damager
            if (damager is Player) checkInteractEvent(event, damager, event.entity)
        }

        private fun <T> checkInteractEvent(event: T, other: Entity?) where T : PlayerEvent, T : Cancellable {
            checkInteractEvent(event, event.player, other)
        }

        private fun checkInteractEvent(event: Cancellable, player: Player, other: Entity?) {
            val config = config
            if (!config.cancelEntityInteractOnAwait && (other == null || !config.detectActualDimensionChanged)) return

            val bukkit = player as CraftPlayer
            val handle = bukkit.handle
            val connection = handle.connection
            if (connection.awaitingPositionFromClient != null) {
                if (config.cancelEntityInteractOnAwait) event.isCancelled = true
                if (config.detectActualDimensionChanged && other != null) {
                    val otherHandle = Accessors.HANDLE_GETTER.getHandle(other)
                    if (Accessors.LEVEL_GETTER.level(otherHandle) === Accessors.LEVEL_GETTER.level(handle)) {
                        handle.hasChangedDimension()
                    }
                }
            }
        }

    }

    private object Accessors {

        val AWAITING_POSITION_FROM_CLIENT = ServerGamePacketListenerImpl::class.java.getDeclaredField("awaitingPositionFromClient").getter
        val AWAITING_TELEPORT_TIME = ServerGamePacketListenerImpl::class.java.getDeclaredField("awaitingTeleportTime").getter
        val TICK_COUNT = ServerGamePacketListenerImpl::class.java.getDeclaredField("tickCount").getter

        val HANDLE_GETTER = versioned<EntityHandleGetter>()
        val LEVEL_GETTER = versioned<EntityLevelGetter>()

        val ServerGamePacketListenerImpl.awaitingPositionFromClient
            get() = AWAITING_POSITION_FROM_CLIENT.invokeExact(this) as Vec3?
        val ServerGamePacketListenerImpl.awaitingTeleportTime
            get() = AWAITING_POSITION_FROM_CLIENT.invokeExact(this) as Int
        val ServerGamePacketListenerImpl.tickCount
            get() = AWAITING_POSITION_FROM_CLIENT.invokeExact(this) as Int


    }

    @Comment("""
        Client can cancel or delay ServerboundAcceptTeleportationPacket sending to server,
        this can lead to a inconsistent chunkMap player position state on server,
        or make the player entity invulnerable after a dimension change,
        but the player can still attack other entities.
    """)
    data class FeatureConfig(
//        val confirmTimeout: Duration = Duration.ofSeconds(30),
        val cancelEntityInteractOnAwait: Boolean = true,
        @Comment("""
            If the entity the player interacted is on the same world with the player,
            then consider the player has changed dimension,
            no matter what is happening on the client.
            This removes the invulnerable state of the player.
        """)
        val detectActualDimensionChanged: Boolean = true,
    ) : BaseFeatureConfiguration(true)

}