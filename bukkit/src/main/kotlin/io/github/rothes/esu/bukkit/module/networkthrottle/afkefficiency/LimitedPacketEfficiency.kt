package io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.*
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.module.networkthrottle.AfkEfficiency
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.util.extension.math.square
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * Ported from Ghost-chu/RIABandwidthSaver .
 */
object LimitedPacketEfficiency: AfkEfficiencyFeature<LimitedPacketEfficiency.FeatureConfig, Unit>() {

    private val limiting = ConcurrentHashMap.newKeySet<Player>()

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents()
    }

    override fun onEnable() {
        super.onEnable()
        PacketEvents.getAPI().eventManager.registerListeners(PacketListeners)
    }

    override fun onDisable() {
        super.onDisable()
        PacketEvents.getAPI().eventManager.unregisterListeners(PacketListeners)
    }

    override fun onEnableEfficiency(playerHolder: AfkEfficiency.PlayerHolder) {
        limiting.add(playerHolder.player)
    }

    override fun onDisableEfficiency(playerHolder: AfkEfficiency.PlayerHolder) {
        limiting.remove(playerHolder.player)
    }

    private object PacketListeners: PacketListenerAbstract(PacketListenerPriority.LOWEST) {

        override fun onPacketSend(e: PacketSendEvent) {
            if (e.isCancelled) return
            when (e.packetType) {
                PacketType.Play.Server.PARTICLE -> {
                    checkCancel(e, config.rangedPackets.processed.particle) {
                        val wrapper = WrapperPlayServerParticle(e)
                        wrapper.position.distanceSqr(e.playerPos)
                    }
                }
                PacketType.Play.Server.SOUND_EFFECT -> {
                    checkCancel(e, config.rangedPackets.processed.soundEffect) {
                        val wrapper = WrapperPlayServerSoundEffect(e)
                        wrapper.effectPosition.distanceSqr(e.playerPos)
                    }
                }
                PacketType.Play.Server.ENTITY_SOUND_EFFECT -> {
                    checkCancel(e, config.rangedPackets.processed.entitySoundEffect) {
                        val wrapper = WrapperPlayServerEntitySoundEffect(e)
                        entityIdDist(e, wrapper.entityId)
                    }
                }
                PacketType.Play.Server.NAMED_SOUND_EFFECT -> {} // Legacy
                PacketType.Play.Server.HURT_ANIMATION -> {
                    checkCancel(e, config.rangedPackets.processed.hurtAnimation) {
                        val wrapper = WrapperPlayServerHurtAnimation(e)
                        entityIdDist(e, wrapper.entityId)
                    }
                }
                PacketType.Play.Server.DAMAGE_EVENT -> {
                    checkCancel(e, config.rangedPackets.processed.damageEvent) {
                        val wrapper = WrapperPlayServerDamageEvent(e)
                        entityIdDist(e, wrapper.entityId)
                    }
                }
                PacketType.Play.Server.ENTITY_HEAD_LOOK -> {
                    checkCancel(e, config.rangedPackets.processed.entityHeadLook) {
                        val wrapper = WrapperPlayServerEntityHeadLook(e)
                        entityIdDist(e, wrapper.entityId)
                    }
                }
                PacketType.Play.Server.SPAWN_EXPERIENCE_ORB -> {} // Legacy
                PacketType.Play.Server.TIME_UPDATE -> {
                    checkCancel(e, config.globalPackets.timeUpdate)
                }
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun checkCancel(e: PacketSendEvent, bool: Boolean) {
            if (!bool && e.getPlayer<Player>().isInAfk()) e.isCancelled = true
        }

        private inline fun checkCancel(e: PacketSendEvent, maxDist: Double, scope: () -> Double) {
            if (maxDist < 0 || !e.getPlayer<Player>().isInAfk()) return
            if (maxDist == 0.0 || maxDist < scope.invoke()) {
                e.isCancelled = true
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun Player.isInAfk(): Boolean {
            return limiting.contains(this)
        }

        private fun Vector3d.distanceSqr(o: Location): Double {
            return (x - o.x).square() + (y - o.y).square() + (z - o.z).square()
        }
        private fun Vector3i.distanceSqr(o: Location): Double {
            return (x - o.x).square() + (y - o.y).square() + (z - o.z).square()
        }

        private fun entityIdDist(e: PacketSendEvent, entityId: Int): Double {
            val loc = e.playerPos
            val entity = SpigotConversionUtil.getEntityById(loc.world, entityId)
            return entity?.location?.distanceSquared(loc) ?: Double.MAX_VALUE // For null entity just cancel it
        }

        private val PacketSendEvent.playerPos: Location
            get() = getPlayer<Player>().location

    }

    data class FeatureConfig(
        @Comment("""
            Limit far away world packets to player.
            Set -1 to disable modifying specified packet, 0 to cancel all specified packet,
             or a distance from player location of allowed packet.
        """)
        val rangedPackets: RangedPackets = RangedPackets(),
        @Comment("""
            Set to false to cancel specified packet.
        """)
        val globalPackets: GlobalPackets = GlobalPackets(),
    ): BaseFeatureConfiguration(true) {

        data class RangedPackets(
            val particle: Double = 0.0,
            val soundEffect: Double = 8.0,
            val entitySoundEffect: Double = 10.0,
            val hurtAnimation: Double = 0.0,
            val damageEvent: Double = 0.0,
            val entityHeadLook: Double = 0.0,
        ) {
            val processed: RangedPackets by lazy(LazyThreadSafetyMode.NONE) {
                copy(
                    particle = particle.square(),
                    soundEffect = soundEffect.square(),
                    entitySoundEffect = entitySoundEffect.square(),
                    hurtAnimation = hurtAnimation.square(),
                    damageEvent = damageEvent.square(),
                    entityHeadLook = entityHeadLook.square(),
                )
            }
        }

        data class GlobalPackets(
            val timeUpdate: Boolean = true,
        )

    }

}