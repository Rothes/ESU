package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18
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
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
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

object ChunkDataThrottle: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    private val FULL_CHUNK = BooleanArray(0)

    private val worldCache = Object2ObjectOpenHashMap<String, WorldCache>()
    private val minimalChunks = hashMapOf<Player, Long2ObjectOpenHashMap<BooleanArray>>()
    private val occludeCache = OccludeCache()

    private fun worldCache(world: World, y: Int) = worldCache.getOrPut(world.name) { WorldCache(world.name) }

    init {
        for (player in Bukkit.getOnlinePlayers()) {
            val miniChunks = player.miniChunks
            data.minimalChunks[player.uniqueId]?.forEach {
//                miniChunks[it.key] = it.value.toHashSet()
            }
        }
        data.minimalChunks.clear()
    }

    fun onEnable() {
        PacketEvents.getAPI().eventManager.registerListener(this)
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun onDisable() {
        PacketEvents.getAPI().eventManager.unregisterListener(this)
        HandlerList.unregisterAll(this)

        if (plugin.isEnabled || plugin.disabledHot) {
            for ((player, map) in minimalChunks.entries) {
                map.forEach { (k, v) ->
                    if (v !== FULL_CHUNK) {
//                        data.minimalChunks.computeIfAbsent(player.uniqueId) { linkedMapOf() }.put(k, v.toList())
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
        sendFullChunk(e.player, chunk.x, chunk.z)
    }

    private val Player.miniChunks
        get() = this@ChunkDataThrottle.minimalChunks.getOrPut(this) { Long2ObjectOpenHashMap() }

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
//                val tm = System.nanoTime()
                val wrapper = WrapperPlayServerChunkData(event)
                val player = event.getPlayer<Player>()
                val column = wrapper.column
                val chunkKey = Chunk.getChunkKey(column.x, column.z)

                val miniChunks = player.miniChunks
                if (miniChunks[chunkKey] === FULL_CHUNK) {
                    // This should be a full chunk
                    miniChunks.remove(chunkKey)
                    return
                }

                val world = player.world
                val chunks = column.chunks

                val minHeight = world.minHeight
                val maxHeight = world.maxHeight
                val height = maxHeight - minHeight
                val maxHeightToProceed = config.chunkDataThrottle.maxHeightToProceed

                val wc = worldCache(world, maxHeight - minHeight)
                val cc = wc.chunks[chunkKey]
                if (cc != null) {
                    val clone = cc.invisible.clone()
                    var i = 0
                    var id = 0
                    out@ for (chunk in chunks) {
                        chunk as Chunk_v1_18
                        val air = chunk.chunkData.palette.stateToId(0)
                        for (y in 0 ..< 16) {
                            for (zAndX in 0 ..< 16 * 16) {
                                if (clone[id]) {
                                    chunk.chunkData.storage?.set(id and 0xfff, air)
                                    chunk.blockCount--
                                }
                                id++
                            }
                            if (++i > maxHeightToProceed) {
                                break@out
                            }
                        }
                    }
                    miniChunks[chunkKey] = clone
//                    println("FROM CACHE! ${System.nanoTime() - tm}")
                    return
                }
                val chunkCache = ChunkCache(BooleanArray(16 * 16 * height))
//                wc.chunks[chunkKey] = chunkCache
//                wc.chunks[chunkKey + 1] ?.let { neighbor ->
//                    chunkCache.xp = neighbor
//                    neighbor.xm = chunkCache
//                }
//                wc.chunks[chunkKey - 1] ?.let { neighbor ->
//                    chunkCache.xm = neighbor
//                    neighbor.xp = chunkCache
//                }
//                wc.chunks[chunkKey + (1 shl 32)] ?.let { neighbor ->
//                    chunkCache.zp = neighbor
//                    neighbor.zm = chunkCache
//                }
//                wc.chunks[chunkKey - (1 shl 32)] ?.let { neighbor ->
//                    chunkCache.zm = neighbor
//                    neighbor.zp = chunkCache
//                }

                val dp = BooleanArray(16 * 16 * height)
                var i = 0
                var id = 0
                out@ for ((index, chunk) in chunks.withIndex()) {
                    chunk as Chunk_v1_18
                    val air = chunk.chunkData.palette.stateToId(0)
                    for (y in 0 ..< 16) {
                        for (z in 0 ..< 16) {
                            for (x in 0 ..< 16) {
                                val blockId = chunk.chunkData.palette.idToState(chunk.chunkData.storage?.get(id and 0xfff) ?: 0)
                                if (blockId != 0 && blockId.occlude) {
                                    dp[id] = true
                                    if ( x >= 2 && i >= 2 && z >= 2 // We want to keep the edge so the algo is simpler
                                        // Check if the block is visible
                                        && dp[id - 0x211] && dp[id - 0x121] && dp[id - 0x112]
                                        && dp[id - 0x011] && dp[id - 0x110] && dp[id - 0x101]) {

                                        if (y == 0) {
                                            val chunk = chunks[index - 1] as Chunk_v1_18
                                            chunk.chunkData.storage.set(id - 0x111 and 0xfff, chunk.chunkData.palette.stateToId(0))
                                            chunk.blockCount--
                                        } else {
                                            chunk.chunkData.storage.set(id - 0x111 and 0xfff, air)
                                            chunk.blockCount--
                                        }
                                        chunkCache.invisible[id - 0x111] = true
                                    }
                                }
                                id++
                            }
                        }
                        if (++i > maxHeightToProceed) {
                            break@out
                        }
                    }
                }
//                println("NO CACHE.... ${System.nanoTime() - tm}")
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
        val playerCache = miniChunks[chunkKey] ?: return
        if (playerCache !== FULL_CHUNK) {
            miniChunks[chunkKey] = FULL_CHUNK
            val nms = (player as CraftPlayer).handle
            val level = nms.serverLevel()
            PlayerChunkSender.sendChunk(nms.connection, level, level.`moonrise$getFullChunkIfLoaded`(x, z))
        }
    }

    private data class ChunkPos(val x: Int, val z: Int)
    private val Long.chunkPos
        get() = ChunkPos((this and 0xffffffffL).toInt(), (this shr 32).toInt())

    private fun blockKeyChunk(x: Int, y: Int, z: Int): Int {
        return x or (z shl 4) or (y shl 8)
    }
    private fun blockKeyChunkAnd(x: Int, y: Int, z: Int): Int {
        return (x and 0xf) or (z and 0xf shl 4) or (y and 0xf shl 8)
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
            val invisible: BooleanArray,
            var xp: ChunkCache? = null,
            var xm: ChunkCache? = null,
            var zp: ChunkCache? = null,
            var zm: ChunkCache? = null,
            var lastAccess: Long = System.currentTimeMillis(),
        ) {
            data class BlockPosition(val chunk: Int, val x: Int, val y: Int, val z: Int)
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