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
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.data
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.module.CommonModule
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import it.unimi.dsi.fastutil.longs.Long2BooleanMap
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
import net.minecraft.server.network.PlayerChunkSender
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

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
        data.minimalChunks.clear()
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