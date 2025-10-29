package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.module.NetworkThrottleModule
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.CullDataManager
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.RaytraceHandler
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.version.VersionUtils.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.RegistryValueSerializers
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.incendo.cloud.annotations.Command
import kotlin.time.Duration.Companion.seconds

object EntityCulling : CommonFeature<EntityCulling.FeatureConfig, EmptyConfiguration>() {

    override val module
        get() = super.module as NetworkThrottleModule
    private var lastThreads = 0
    private var coroutine: ExecutorCoroutineDispatcher? = null
    private val raytraceHandler = try {
        RegistryValueSerializers.instance
        RaytraceHandler::class.java.versioned().also {
            registerFeature(it)
        }
    } catch (e: Exception) {
        e
    }

    private var previousElapsedTime = 0L
    private var previousDelayTime = 0L

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (config.raytraceThreads < 1) {
                plugin.err("[EntityCulling] At least one raytrace thread is required to enable this feature.")
                return Feature.AvailableCheck.fail { "At least one raytrace thread is required!".message }
            }
            if (raytraceHandler !is RaytraceHandler<*, *>) {
                plugin.err("[EntityCulling] Server is not supported.", raytraceHandler as Throwable)
                return Feature.AvailableCheck.fail { "Server is not supported".message }
            }
            null
        }
    }

    override fun onReload() {
        super.onReload()
        if (enabled) {
            if (lastThreads != config.raytraceThreads) {
                start()
            }
        }
    }

    override fun onEnable() {
        start()
        registerCommands(object {
            @Command("esu networkThrottle entityCulling benchmark")
            @ShortPerm
            fun benchmark(sender: User) {
                val user = sender as PlayerUser
                val player = user.player
                sender.message("Preparing data at this spot...")
                val loc = player.eyeLocation
                val world = loc.world
                val maxI = 100_000_00
                val viewDistance = world.viewDistance - 2
                val data = Array(maxI) {
                    loc.clone().add(
                        (-16 * viewDistance .. 16 * viewDistance).random().toDouble(),
                        (world.minHeight .. loc.blockY + 48).random().toDouble(),
                        (-16 * viewDistance .. 16 * viewDistance).random().toDouble(),
                    ).toVector()
                }
                val from = loc.toVector()
                var i = 0
                sender.message("Running benchmark")
                val raytraceHandler = raytraceHandler as RaytraceHandler<*, *>
                runBlocking {
                    var count = 0
                    val jobs = buildList(4) {
                        repeat(4) {
                            val job = launch(coroutine!!) {
                                while (isActive) {
                                    var get = ++i
                                    if (i >= maxI) {
                                        i = 0
                                        get = 0
                                    }
                                    raytraceHandler.raytrace(from, data[get], world)
                                    count++
                                }
                            }
                            add(job)
                        }
                    }
                    delay(1.seconds)
                    jobs.forEach { it.cancel() }
                    sender.message("Raytrace $count times in 1 seconds")
                    sender.message("Max of ${count / 7 / 20} entities per tick")
                    sender.message("Test result is for reference only.")
                }
            }

            @Command("esu networkThrottle entityCulling stats")
            @ShortPerm
            fun stats(sender: User) {
                sender.message("previousElapsedTime: ${previousElapsedTime}ms ; previousDelayTime: ${previousDelayTime}ms")
            }
        })
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        coroutine?.close()
        coroutine = null
        lastThreads = 0
        Listeners.unregister()
        CullDataManager.shutdown()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun start() {
        coroutine?.close()
        val nThreads = config.raytraceThreads
        val context = newFixedThreadPoolContext(nThreads, "ESU-EntityCulling")
        val raytraceHandler = raytraceHandler as RaytraceHandler<*, *>
        CoroutineScope(context).launch {
            while (isActive) {
                val millis = System.currentTimeMillis()
                Bukkit.getOnlinePlayers().map { bukkitPlayer ->
                    launch {
                        try {
                            raytraceHandler.updatePlayer(bukkitPlayer, CullDataManager[bukkitPlayer])
                        } catch (e: Throwable) {
                            plugin.err("[EntityCulling] Failed to update player ${bukkitPlayer.name}", e)
                        }
                    }
                }.joinAll()
                val elapsed = System.currentTimeMillis() - millis
                val delay = config.millisBetweenUpdates - elapsed
                previousElapsedTime = elapsed
                previousDelayTime = delay
                delay(delay)
            }
        }
        lastThreads = nThreads
        coroutine = context
    }

    private object Listeners: Listener {

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            CullDataManager.remove(event.player)
        }

    }

    @Comment("""
        [ EXPERIMENTAL ]
        Smart Occlusion Culling to save upload bandwidth.
        Plugin will hide invisible entities to players.
    """)
    data class FeatureConfig(
        @Comment("Asynchronous threads used to calculate visibility. More to update faster.")
        val raytraceThreads: Int = Runtime.getRuntime().availableProcessors() / 3,
        @Comment("""
            Max updates for each player per second.
            More means greater immediacy, but also higher cpu usage.
        """)
        val updatesPerSecond: Int = 15,
    ): BaseFeatureConfiguration() {

        val millisBetweenUpdates by lazy { 1000 / updatesPerSecond }
    }

}