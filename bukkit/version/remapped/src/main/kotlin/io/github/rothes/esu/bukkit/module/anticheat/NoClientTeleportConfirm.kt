/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.anticheat

import io.github.rothes.esu.bukkit.module.anticheat.NoClientTeleportConfirm.Accessors.awaitingPositionFromClient
import io.github.rothes.esu.bukkit.module.anticheat.NoClientTeleportConfirm.Accessors.awaitingTeleportTime
import io.github.rothes.esu.bukkit.module.anticheat.NoClientTeleportConfirm.Accessors.tickCount
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.phys.Vec3
import org.bukkit.craftbukkit.entity.CraftPlayer
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
            checkInteractEvent(event, false)
        }
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onInteract(event: PlayerInteractEntityEvent) {
            checkInteractEvent(event, true)
        }
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun onInteract(event: EntityDamageByEntityEvent) {
            val damager = event.damager
            if (damager is Player) checkInteractEvent(event, damager, true)
        }

        private fun <T> checkInteractEvent(event: T, isEntity: Boolean) where T : PlayerEvent, T : Cancellable {
            checkInteractEvent(event, event.player, isEntity)
        }

        private fun checkInteractEvent(event: Cancellable, player: Player, isEntity: Boolean) {
            val config = config.entityInteractExemptionTicks
            if (config.cancelInteract < 0 && (!isEntity || config.assumeDimensionChanged < 0)) return

            val bukkit = player as CraftPlayer
            val handle = bukkit.handle
            val connection = handle.connection
            if (connection.awaitingPositionFromClient != null) {
                /*
                    TODO:
                    ServerGamePacketListenerImpl#updateAwaitingTeleport teleports player again when the
                    player still attempts to move after awaitTicks > 20, this resets awaitTicks to zero.
                    Try tracking the first awaitingTeleportTime, release it after receiving a legal ServerboundAcceptTeleportationPacket.
                    * This is not necessary on Paper, they have disabled updateAwaitingTeleport.
                */
                val awaitTicks = connection.tickCount - connection.awaitingTeleportTime
                if (config.cancelInteract in 0..awaitTicks) event.isCancelled = true
                // Vanilla only pass entity interact on the same level, so we can safely assume the client has actually
                // completed the teleport. Check ServerGamePacketListenerImpl#handleInteract(ServerboundInteractPacket)
                if (config.assumeDimensionChanged in (0..awaitTicks)) handle.hasChangedDimension()
            }
        }

    }

    private object Accessors {

        val AWAITING_POSITION_FROM_CLIENT = ServerGamePacketListenerImpl::class.java.getDeclaredField("awaitingPositionFromClient").getter
        val AWAITING_TELEPORT_TIME = ServerGamePacketListenerImpl::class.java.getDeclaredField("awaitingTeleportTime").getter
        val TICK_COUNT = ServerGamePacketListenerImpl::class.java.getDeclaredField("tickCount").getter

        val ServerGamePacketListenerImpl.awaitingPositionFromClient
            get() = AWAITING_POSITION_FROM_CLIENT.invokeExact(this) as Vec3?
        val ServerGamePacketListenerImpl.awaitingTeleportTime
            get() = AWAITING_TELEPORT_TIME.invokeExact(this) as Int
        val ServerGamePacketListenerImpl.tickCount
            get() = TICK_COUNT.invokeExact(this) as Int


    }

    @Comment("""
        Client can cancel or delay ServerboundAcceptTeleportationPacket sending to server,
        this can lead to a inconsistent chunkMap player position state on server,
        or make the player entity invulnerable after a dimension change,
        but the player can still attack other entities.
    """)
    data class FeatureConfig(
        @Comment("""
            Control the maximum exemption game ticks after a teleport, before the client
            notify the server it has been confirmed.
            Set to a negative value to disable specific check.
            Set to a low value may screw clients with high network RTT.
        """)
        val entityInteractExemptionTicks: EntityInteractTicks = EntityInteractTicks(),
    ) : BaseFeatureConfiguration(true) {

        data class EntityInteractTicks(
            @Comment("""
                Cancel the interact with entities, this applies to both attack and right click.
            """)
            val cancelInteract: Int = 15,
            @Comment("""
                If the entity the player interacted is on the same world with the player,
                then consider the player has changed dimension,
                no matter what is happening on the client.
                This removes the invulnerable state of the player.
            """)
            val assumeDimensionChanged: Int = 10,
        )
    }

}