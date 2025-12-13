package io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.module.networkthrottle.AfkEfficiency
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.PlayerEntityVisibilityHandler
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.extension.createChild
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.delayedTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityValidTester
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.util.extension.math.square
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

object EntityTrackingEfficiency: AfkEfficiencyFeature<EntityTrackingEfficiency.FeatureConfig, Unit>() {

    private val playerData = ConcurrentHashMap<Player, EfficiencyData>()
    private val pl = bootstrap.createChild(name = "$name-EntityTrackingEfficiency")

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (ServerCompatibility.serverVersion < 19 || !ServerCompatibility.isPaper)
                return errFail { "Paper 1.19+ is required to enable this.".message }
            null
        }
    }

    override fun onEnable() {
        Listeners.register()
        super.onEnable()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
        for (data in playerData.values) {
            data.shutdown()
        }
        playerData.clear()
    }

    override fun onEnableEfficiency(playerHolder: AfkEfficiency.PlayerHolder) {
        val player = playerHolder.player
        playerData[player] = EfficiencyData(player)
    }

    override fun onDisableEfficiency(playerHolder: AfkEfficiency.PlayerHolder) {
        playerData.remove(playerHolder.player)?.shutdown()
    }

    private object Listeners: Listener {

        @EventHandler
        fun onTrack(e: PlayerTrackEntityEvent) {
            val entity = e.entity
            if (entity is Player) return
            val player = e.player
            if (player.location.distanceSquared(entity.location) < config.visibleEntityDistanceSquared) return

            val data = playerData[player] ?: return
            data.hiddenEntities.add(entity)
            player.hideEntity(pl, entity) // Call hideEntity otherwise showEntity doesn't re-track the entity.
            e.isCancelled = true
        }
    }

    private class EfficiencyData(val player: Player) {

        companion object {
            private val VISIBILITY_HANDLER by Versioned(PlayerEntityVisibilityHandler::class.java)
            private val VALID_TESTER by Versioned(EntityValidTester::class.java)
        }

        @Volatile private var shutdown = false
        private var ticked = 0L
        private var task: ScheduledTask = schedule()

        val hiddenEntities = LinkedList<Entity>()

        init {
            start()
        }

        fun start() {
            player.syncTick {
                val loc = player.location
                val dist = config.visibleEntityDistanceSquared
                for (chunk in player.sentChunks) {
                    val entities = chunk.entities
                    for (entity in entities) {
                        if (entity is Player) continue
                        if (entity.location.distanceSquared(loc) > dist) {
                            hiddenEntities.add(entity)
                            player.hideEntity(pl, entity)
                        }
                    }
                }
            }
        }

        fun update() {
            val loc = player.location
            val visibleDist = config.visibleEntityDistanceSquared
            val iterator = hiddenEntities.iterator()
            for (entity in iterator) {
                if (!VALID_TESTER.isValid(entity)) {
                    // The entity is removed from the world
                    iterator.remove()
                    continue
                }

                val other = entity.location
                if (other.world !== loc.world) {
                    // Entity is not on same world anymore
                    VISIBILITY_HANDLER.forceShowEntity(player, entity, pl)
                } else {
                    val dist = entity.location.distanceSquared(loc)
                    if (dist <= visibleDist) {
                        player.showEntity(pl, entity)
                    } else if (dist > 1024 * 1024) {
                        // Entity is far away
                        if (entity.checkTickThread()) {
                            // May be just huge Y diff, still need to use this to re-track entity
                            player.showEntity(pl, entity)
                        } else {
                            VISIBILITY_HANDLER.forceShowEntity(player, entity, pl)
                        }
                    } else {
                        // Still hide this entity
                        continue
                    }
                }
                iterator.remove()
            }
        }

        fun shutdown() {
            shutdown = true
            task.cancel()
            for (entity in hiddenEntities) {
                if (VALID_TESTER.isValid(entity)) {
                    if (entity.checkTickThread())
                        player.showEntity(pl, entity)
                    else
                        VISIBILITY_HANDLER.forceShowEntity(player, entity, pl)
                }
            }
            hiddenEntities.clear()
        }

        private fun schedule(): ScheduledTask {
            fun tick() {
                if (shutdown) return
                try {
                    update()
                } catch (e: Throwable) {
                    pl.logger.log(Level.SEVERE, "Failed to update ${player.name}", e)
                }
                schedule()
            }
            val interval = config.updateIntervalTicks
            ticked += interval

            task = player.delayedTick(interval) {
                tick()
            } ?: error("Failed to schedule update task for player ${player.name}")
            return task
        }

    }


    @Comment("""
        Hide all far away entities from player.
    """)
    data class FeatureConfig(
        @Comment("Player could see nearby entities in this distance")
        val visibleEntityDistance: Double = 7.0,
        @Comment("Server game tick interval to update player nearby entities")
        val updateIntervalTicks: Long = 5,
    ): BaseFeatureConfiguration(true) {

        val visibleEntityDistanceSquared by lazy { visibleEntityDistance.square() }
    }

}