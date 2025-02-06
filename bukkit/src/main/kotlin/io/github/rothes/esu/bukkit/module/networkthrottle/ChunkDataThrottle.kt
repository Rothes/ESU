package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.data
import io.github.rothes.esu.bukkit.module.networkthrottle.ChunkDataThrottle.WorldCache.ChunkCache
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.utils.JvmArrayUtils
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import it.unimi.dsi.fastutil.longs.Long2BooleanMap
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.server.network.PlayerChunkSender
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.collections.getOrPut

object ChunkDataThrottle: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    private val worldCache = Object2ObjectOpenHashMap<String, WorldCache>()
    private val minimalChunks = hashMapOf<Player, Long2BooleanMap>()
    private val occludeCache = OccludeCache()

    private var tick = 0
    private var tickTask: ScheduledTask? = null

    private fun worldCache(world: World) = worldCache.getOrPut(world.name) { WorldCache(world.name) }

    init {
        for (player in Bukkit.getOnlinePlayers()) {
            val miniChunks = player.miniChunks
            data.minimalChunks[player.uniqueId]?.forEach {
                miniChunks[it] = true
            }
        }
        data.minimalChunks.clear()
    }

    fun onEnable() {
        PacketEvents.getAPI().eventManager.registerListener(this)
        Bukkit.getPluginManager().registerEvents(this, plugin)

        tickTask = Scheduler.async(1, 1) {
            tick++
            val expireTicks = config.chunkDataThrottle.cacheExpireTicks
            worldCache.
        }
    }

    fun onDisable() {
        PacketEvents.getAPI().eventManager.unregisterListener(this)
        HandlerList.unregisterAll(this)

        if (plugin.isEnabled || plugin.disabledHot) {
            for ((player, map) in minimalChunks.entries) {
                map.forEach { (k, v) ->
                    if (v) {
                        data.minimalChunks.computeIfAbsent(player.uniqueId) { arrayListOf() }.add(k)
                    }
                }
            }
        }
        tickTask?.cancel()
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
        sendFullChunk(e.player, chunk.x, chunk.z)
    }

    private val Player.miniChunks
        get() = this@ChunkDataThrottle.minimalChunks.getOrPut(this) { Long2BooleanOpenHashMap() }

    override fun onPacketSend(event: PacketSendEvent) {
        if (!config.chunkDataThrottle.enabled) {
            return
        }
        when (event.packetType) {
            PacketType.Play.Server.EXPLOSION    -> {
                val wrapper = WrapperPlayServerExplosion(event)
                when (wrapper.blockInteraction) {
                    WrapperPlayServerExplosion.BlockInteraction.DESTROY_BLOCKS,
                    WrapperPlayServerExplosion.BlockInteraction.DECAY_DESTROYED_BLOCKS -> {
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
            PacketType.Play.Server.CHUNK_DATA   -> {
                val tm = System.nanoTime()
                val wrapper = WrapperPlayServerChunkData(event)
                val player = event.getPlayer<Player>()
                val column = wrapper.column
                val chunkKey = Chunk.getChunkKey(column.x, column.z)

                val miniChunks = player.miniChunks
                if (miniChunks.getOrDefault(chunkKey, true) == false) {
                    // This should be a full chunk
                    miniChunks.remove(chunkKey)
                    return
                }
                miniChunks[chunkKey] = true

                val world = player.world
                val chunks = column.chunks

                val wc = worldCache(world)
                val cc = wc.chunks[chunkKey]
                if (cc != null) {
                    cc.lastAccess = tick
                    for (position in cc.invisibleList) {
                        val (chunk, x, y, z) = position
                        chunks[chunk].set(x, y, z, 0)
                    }
                    println("FROM CACHE! ${System.nanoTime() - tm}")
                    return
                }

                val minHeight = world.minHeight
                val maxHeight = world.maxHeight
                val chunkCache = ChunkCache(JvmArrayUtils.newBoolArray(16, maxHeight - minHeight, 16))
                wc.chunks[chunkKey] = chunkCache
                wc.chunks[chunkKey + 1] ?.let { neighbor ->
                    chunkCache.xp = neighbor
                    neighbor.xm = chunkCache
                }
                wc.chunks[chunkKey - 1] ?.let { neighbor ->
                    chunkCache.xm = neighbor
                    neighbor.xp = chunkCache
                }
                wc.chunks[chunkKey + 1 shl 32] ?.let { neighbor ->
                    chunkCache.zp = neighbor
                    neighbor.zm = chunkCache
                }
                wc.chunks[chunkKey - 1 shl 32] ?.let { neighbor ->
                    chunkCache.zm = neighbor
                    neighbor.zp = chunkCache
                }

                val maxHeightToProceed = config.chunkDataThrottle.maxHeightToProceed
                val dp = JvmArrayUtils.newBoolArray(16, maxHeight - minHeight, 16)
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
                                            chunkCache.invisibleList.add(ChunkCache.BlockPosition(index - 1, x - 1, 15, z - 1))
                                            chunks[index - 1].set(x - 1, 15, z - 1, 0)
                                        } else {
                                            chunkCache.invisibleList.add(ChunkCache.BlockPosition(index, x - 1, y - 1, z - 1))
                                            chunk.set(x - 1, y - 1, z - 1, 0)
                                        }
                                        chunkCache.invisible[x - 1][i - 1][z - 1] = true
                                    }
                                }
                            }
                        }
                        if (++i > maxHeightToProceed) {
                            break@out
                        }
                    }
                }
                println("NO CACHE.... ${System.nanoTime() - tm}")
            }
        }
    }

    private fun sendFullChunk(player: Player, blockLocation: Vector3i) {
        val x = blockLocation.x shr 4
        val z = blockLocation.z shr 4
        sendFullChunk(player, x, z)
    }

    private fun sendFullChunk(player: Player, x: Int, z: Int) {
        val miniChunks = player.miniChunks
        val chunkKey = Chunk.getChunkKey(x, z)
        val minimal = miniChunks[chunkKey]
        if (minimal) {
            miniChunks[chunkKey] = false
            val nms = (player as CraftPlayer).handle
            val level = nms.serverLevel()
            PlayerChunkSender.sendChunk(nms.connection, level, level.`moonrise$getFullChunkIfLoaded`(x, z))
        }
    }

    private data class ChunkPos(val x: Int, val z: Int)
    private val Long.chunkPos
        get() = ChunkPos((this and 0xffffffffL).toInt(), (this shr 32).toInt())

    private fun blockKeyChunk(x: Int, y: Int, z: Int): Int {
        return z or (x shl 4) or (y shl 8)
    }
    private data class ChunkBlockPos(val x: Int, val y: Int, val z: Int)

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

    data class WorldCache(
        val worldName: String,
        val chunks: Long2ObjectMap<ChunkCache> = Long2ObjectOpenHashMap(),
    ) {

        data class ChunkCache(
            val invisible: Array<Array<BooleanArray>>,
            val invisibleList: MutableList<BlockPosition> = arrayListOf(),
            val updateHandles: ArrayDeque<UpdateHandle> = ArrayDeque(),
            var xp: ChunkCache? = null,
            var xm: ChunkCache? = null,
            var zp: ChunkCache? = null,
            var zm: ChunkCache? = null,
            var lastAccess: Int = tick,
        ) {
            data class BlockPosition(val chunk: Int, val x: Int, val y: Int, val z: Int)
            data class UpdateHandle(val x: Int, val y: Int, val z: Int, val requestedTick: Int)
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