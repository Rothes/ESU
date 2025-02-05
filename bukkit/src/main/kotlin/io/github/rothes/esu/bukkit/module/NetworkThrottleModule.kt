package io.github.rothes.esu.bukkit.module

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion.BlockInteraction
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.netty.buffer.ByteBuf
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import it.unimi.dsi.fastutil.longs.Long2BooleanMap
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
import net.minecraft.server.network.PlayerChunkSender
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.incendo.cloud.annotations.Command
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

object NetworkThrottleModule: BukkitModule<NetworkThrottleModule.ModuleConfig, NetworkThrottleModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    private lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override fun enable() {
        data = ConfigLoader.load(dataPath)
        ConfigLoader.save(dataPath, data)
        registerCommands(object {
            @Command("network analyser start")
            @ShortPerm("analyser")
            fun analyserStart(sender: User) {
                if (Analyser.start()) {
                    sender.message(locale, { analyser.started })
                } else {
                    sender.message(locale, { analyser.alreadyStarted })
                }
            }

            @Command("network analyser stop")
            @ShortPerm("analyser")
            fun analyserStop(sender: User) {
                if (Analyser.stop()) {
                    sender.message(locale, { analyser.stopped })
                } else {
                    sender.message(locale, { analyser.alreadyStopped })
                }
            }

            @Command("network analyser view")
            @ShortPerm("analyser")
            fun analyserView(sender: User) {
                val entries = Analyser.records
                    .mapValues { it.value.toList().sumOf { it.size.toLong() } }
                    .entries.sortedByDescending { it.value }
                if (entries.isEmpty()) {
                    sender.message(locale, { analyser.view.noData })
                    return
                }
                sender.message(locale, { analyser.view.header })
                for ((k, bytes) in entries.take(7)) {
                    sender.message(locale, { analyser.view.entry },
                        unparsed("packet-type", k.name.lowercase()),
                        unparsed("size", when {
                            bytes >= 1 shl 30 -> "%.1f GB".format(bytes.toDouble() / (1 shl 30))
                            bytes >= 1 shl 20 -> "%.1f MB".format(bytes.toDouble() / (1 shl 20))
                            bytes >= 1 shl 10 -> "%.1f KB".format(bytes.toDouble() / (1 shl 10))
                            else              -> "$bytes bytes"
                        }))
                }
                sender.message(locale, { analyser.view.footer },
                    duration(
                        (if (Analyser.running) {
                            System.currentTimeMillis() - Analyser.startTime
                        } else {
                            Analyser.stopTime - Analyser.startTime
                        }).milliseconds, sender
                    ))
            }
        })
        Analyser // Init this
        PacketEvents.getAPI().eventManager.registerListener(ChunkDataThrottle)
        Bukkit.getPluginManager().registerEvents(ChunkDataThrottle, plugin)
        data.minimalChunks.clear()
        ConfigLoader.save(dataPath, data)
    }

    override fun disable() {
        super.disable()
        PacketEvents.getAPI().eventManager.unregisterListener(ChunkDataThrottle)
        HandlerList.unregisterAll(ChunkDataThrottle)
        ChunkDataThrottle.onDisable()
        Analyser.stop()
        ConfigLoader.save(dataPath, data)
    }

    object ChunkDataThrottle: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

        private val minimalChunks = hashMapOf<Player, Long2BooleanMap>()
        private val occludeCache = OccludeCache()

        init {
            for (player in Bukkit.getOnlinePlayers()) {
                val miniChunks = player.miniChunks
                data.minimalChunks[player.uniqueId]?.forEach {
                    miniChunks[it] = false
                }
            }
        }

        fun onDisable() {
            if (plugin.isEnabled || plugin.disabledHot) {
                for ((player, map) in minimalChunks.entries) {
                    map.forEach { (k, v) ->
                        if (!v) {
                            data.minimalChunks.computeIfAbsent(player.uniqueId) { arrayListOf() }.add(k)
                        }
                    }
                }
            }
        }

        @EventHandler
        fun onJoin(e: PlayerJoinEvent) {
            e.player.miniChunks
        }

        @EventHandler
        fun onQuit(e: PlayerQuitEvent) {
            minimalChunks.remove(e.player)
        }

        @EventHandler
        fun onChunkUnload(e: PlayerChunkUnloadEvent) {
            e.player.miniChunks.remove(e.chunk.chunkKey)
        }

        @EventHandler
        fun onInteract(e: PlayerInteractEvent) {
            val chunk = e.clickedBlock?.chunk ?: return
            val cache = e.player.miniChunks
            val fullSent = cache.getOrElse(chunk.chunkKey) { true }
            if (!fullSent) {
                cache[chunk.chunkKey] = true
                val nms = (e.player as CraftPlayer).handle
                PlayerChunkSender.sendChunk(nms.connection, nms.serverLevel(), nms.serverLevel().`moonrise$getFullChunkIfLoaded`(chunk.x, chunk.z))
            }
        }

        private val Player.miniChunks
            get() = this@ChunkDataThrottle.minimalChunks.getOrPut(this) { Long2BooleanOpenHashMap() }


        override fun onPacketSend(event: PacketSendEvent) {
            if (!config.chunkDataThrottle.enabled) {
                return
            }
            when (event.packetType) {
                PacketType.Play.Server.EXPLOSION -> {
                    val wrapper = WrapperPlayServerExplosion(event)
                    when (wrapper.blockInteraction) {
                        BlockInteraction.DESTROY_BLOCKS, BlockInteraction.DECAY_DESTROYED_BLOCKS -> {
                            val player = event.getPlayer<Player>()
                            for (location in wrapper.records)
                                sendFullChunk(player, location)
                        }
                        else -> return
                    }
                }
                PacketType.Play.Server.BLOCK_CHANGE -> {
                    val wrapper = WrapperPlayServerBlockChange(event)
                    val player = event.getPlayer<Player>()
                    if (wrapper.blockId != 0) {
                        // Only send full chunk if blocks get broken
                        return
                    }
                    sendFullChunk(player, wrapper.blockPosition)
                }
                PacketType.Play.Server.CHUNK_DATA -> {
                    val wrapper = WrapperPlayServerChunkData(event)
                    val player = event.getPlayer<Player>()
                    val column = wrapper.column
                    val chunkKey = Chunk.getChunkKey(column.x, column.z)

                    val miniChunks = player.miniChunks
                    if (miniChunks[chunkKey] == true) {
                        // This should be a full chunk
                        return
                    }
                    miniChunks[chunkKey] = false

                    val world = player.world
                    val minHeight = world.minHeight
                    val maxHeight = world.maxHeight

                    val maxHeightToProceed = config.chunkDataThrottle.maxHeightToProceed
                    val dp = Array(16) { Array(maxHeight - minHeight) { BooleanArray(16) } }
                    val chunks = column.chunks
                    var i = 0
                    out@ for ((index, chunk) in chunks.withIndex()) {
                        for (y in 0 ..< 16) {
                            for (x in 0 ..< 16) {
                                for (z in 0 ..< 16) {
                                    val blockId = chunk.getBlockId(x, y, z)
                                    if (blockId == 0)
                                        continue

                                    if (blockId.occlude) {
                                        dp[x][i][z] = true
                                        if ( x >= 2 && i >= 2 && z >= 2 // We want to keep the edge so the algo is simpler
                                            // Check if the block is visible
                                            && dp[x - 2][i - 1][z - 1] && dp[x - 1][i - 2][z - 1] && dp[x - 1][i - 1][z - 2]
                                            && dp[x][i - 1][z - 1] && dp[x - 1][i - 1][z] && dp[x - 1][i][z - 1]) {

                                            if (y == 0) {
                                                chunks[index - 1]
                                                    .set(x - 1, 15   , z - 1, 0)
                                            } else {
                                                chunk
                                                    .set(x - 1, y - 1, z - 1, 0)
                                            }
                                        }
                                    }
                                }
                            }
                            if (++i > maxHeightToProceed) {
                                break@out
                            }
                        }
                    }
                }
            }
        }

        private fun sendFullChunk(player: Player, blockLocation: Vector3i) {
            val x = blockLocation.x shr 4
            val z = blockLocation.z shr 4
            val chunkKey = Chunk.getChunkKey(x, z)
            val miniChunks = player.miniChunks
            if (!miniChunks.getOrDefault(chunkKey, true)) {
                miniChunks[chunkKey] = true
                val nms = (player as CraftPlayer).handle
                val level = nms.serverLevel()
                PlayerChunkSender.sendChunk(nms.connection, level, level.`moonrise$getFullChunkIfLoaded`(x, z))
            }
        }

        private val Int.occlude
            get() = if (occludeCache.cached[this]) {
                occludeCache.value[this]
            } else {
                cacheOcclude(this)
            }

        private fun cacheOcclude(blockId: Int): Boolean {
            val wrapped = WrappedBlockState.getByGlobalId(PacketEvents.getAPI().serverManager.version.toClientVersion(), blockId, false)
            val material = SpigotConversionUtil.toBukkitBlockData(wrapped).material
            return material.isOccluding.also {
                occludeCache.cached[blockId] = true
                occludeCache.value[blockId] = it
            }
        }

        data class OccludeCache(
            val cached: BooleanArray = BooleanArray(1 shl 16),
            val value: BooleanArray = BooleanArray(1 shl 16),
        ) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as OccludeCache

                if (!cached.contentEquals(other.cached)) return false
                if (!value.contentEquals(other.value)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = cached.contentHashCode()
                result = 31 * result + value.contentHashCode()
                return result
            }
        }
    }

    object Analyser {
        var running: Boolean = false
            private set
        var startTime: Long = 0
            private set
        var stopTime: Long = 0
            private set

        val records = hashMapOf<PacketTypeCommon, MutableList<PacketRecord>>()

        fun start(): Boolean {
            if (running) return false
            running = true
            startTime = System.currentTimeMillis()
            records.clear()
            PacketEvents.getAPI().eventManager.registerListener(AnalyserPacketListener)
            return true
        }

        fun stop(): Boolean {
            if (!running) return false
            running = false
            stopTime = System.currentTimeMillis()
            PacketEvents.getAPI().eventManager.unregisterListener(AnalyserPacketListener)
            return true
        }

        data class PacketRecord(
            val size: Int,
        )

        object AnalyserPacketListener: PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            override fun onPacketSend(event: PacketSendEvent) {
                val records = records.computeIfAbsent(event.packetType) { arrayListOf() }
                records.add(PacketRecord((event.byteBuf as ByteBuf).capacity()))
            }
        }
    }

    data class ModuleData(
        val minimalChunks: MutableMap<UUID, MutableList<Long>> = linkedMapOf(),
    )

    data class ModuleConfig(
        @field:Comment("Helps to reduce chunk upload bandwidth.\n" +
                "Plugin will only send visible blocks if players are moving fast,\n" +
                "Once they interact with blocks, we send a full chunk data again.")
        val chunkDataThrottle: ChunkDataThrottle = ChunkDataThrottle(),
    ): BaseModuleConfiguration() {

        data class ChunkDataThrottle(
            val enabled: Boolean = false,
            val maxHeightToProceed: Int = 140,
        )
    }

    data class ModuleLang(
        val analyser: Analyser = Analyser(),
    ): ConfigurationPart {

        data class Analyser(
            val started: MessageData = "<pc>Started the analyser.".message,
            val stopped: MessageData = "<pc>Stopped the analyser.".message,
            val alreadyStarted: MessageData = "<ec>The analyser is already running.".message,
            val alreadyStopped: MessageData = "<ec>The analyser is already stopped.".message,

            val view: View = View(),
        ) {

            data class View(
                val noData: MessageData = "<pc>There's no data for view.".message,
                val header: MessageData = "<pdc>[Packet Type]<pc>: <sc>[size]".message,
                val entry: MessageData = "<tdc><packet-type><tc>: <sdc><size>".message,
                val footer: MessageData = "<pc>The analyser has been running for <duration>".message,
            )
        }
    }



}