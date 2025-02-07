package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18
import com.github.retrooper.packetevents.protocol.world.chunk.palette.ListPalette
import com.github.retrooper.packetevents.protocol.world.chunk.palette.MapPalette
import com.github.retrooper.packetevents.protocol.world.chunk.palette.SingletonPalette
import com.github.retrooper.packetevents.protocol.world.chunk.storage.BaseStorage
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.data
import io.github.rothes.esu.bukkit.plugin
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.server.network.PlayerChunkSender
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object ChunkDataThrottle: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    private val FULL_CHUNK = BooleanArray(0)

    private val minimalChunks = hashMapOf<Player, Long2ObjectOpenHashMap<BooleanArray>>()
    private val occludeCache = OccludeCache()
    val counter = Counter()

    fun onEnable() {
        for (player in Bukkit.getOnlinePlayers()) {
            val hotData = data.minimalChunks[player.uniqueId]
            if (hotData != null) {
                val world = player.world
                val minHeight = world.minHeight
                val maxHeight = world.maxHeight
                val height = maxHeight - minHeight
                // We don't persist the invisible blocks data so let's assume all invisible.
                val array = BooleanArray(16 * 16 * height) { true }
                val miniChunks = player.miniChunks
                for (chunkKey in hotData) {
                    miniChunks[chunkKey] = array.clone()
                }
            }
        }
        data.minimalChunks.clear()
        PacketEvents.getAPI().eventManager.registerListener(this)
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun onDisable() {
        PacketEvents.getAPI().eventManager.unregisterListener(this)
        HandlerList.unregisterAll(this)

        if (plugin.isEnabled || plugin.disabledHot) {
            for ((player, map) in minimalChunks.entries) {
                map.forEach { (k, v) ->
                    if (v.contains(true)) {
                        data.minimalChunks.computeIfAbsent(player.uniqueId) { arrayListOf() }.add(k)
                    }
                }
            }
        }
        minimalChunks.clear()
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        e.player.miniChunks
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        minimalChunks.remove(e.player)
    }

    private val Player.miniChunks
        get() = this@ChunkDataThrottle.minimalChunks.getOrPut(this) { Long2ObjectOpenHashMap() }

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (!config.chunkDataThrottle.enabled) {
            return
        }
        when (event.packetType) {
            PacketType.Play.Client.PLAYER_DIGGING -> {
                val wrapper = WrapperPlayClientPlayerDigging(event)
                if (wrapper.action == DiggingAction.START_DIGGING) {
                    wrapper.blockPosition
                    checkBlockUpdate(event.getPlayer<Player>(), wrapper.blockPosition)
                }
            }
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        if (!config.chunkDataThrottle.enabled) {
            return
        }
        when (event.packetType) {
            PacketType.Play.Server.UNLOAD_CHUNK -> {
                val wrapper = WrapperPlayServerUnloadChunk(event)
                event.getPlayer<Player>().miniChunks.remove(Chunk.getChunkKey(wrapper.chunkX, wrapper.chunkZ))
            }
            PacketType.Play.Server.MULTI_BLOCK_CHANGE -> {
                val wrapper = WrapperPlayServerMultiBlockChange(event)
                val player = event.getPlayer<Player>()
                val world = player.world
                val minHeight = world.minHeight
                for (block in wrapper.blocks) {
                    if (block.blockId.occlude) {
                        // Only check full chunk if blocks get broken or transformed to non-occlude
                        continue
                    }
                    if (checkBlockUpdate(player, block.x, block.y, block.z, minHeight)) return
                }
            }
            PacketType.Play.Server.BLOCK_CHANGE -> {
                val wrapper = WrapperPlayServerBlockChange(event)
                if (wrapper.blockId.occlude) {
                    // Only check full chunk if blocks get broken or transformed to non-occlude
                    return
                }
                checkBlockUpdate(event.getPlayer<Player>(), wrapper.blockPosition)
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

                val sections = column.chunks
                val height = event.user.totalWorldHeight
                val occlude = BooleanArray(16 * 16 * height)
                val invisible = BooleanArray(16 * 16 * height)

                var id = 0
                out@ for ((index, section) in sections.withIndex()) {
                    section as Chunk_v1_18
                    val palette = section.chunkData.palette
                    when (palette) {
                        is SingletonPalette -> {
                            // This section contains only one block type, and it's already the smallest way.
                            if (palette.idToState(0).occlude) {
                                forChunk2D { x, z ->
                                    handleOccludePrevSection(occlude, invisible, index, x, z, id, sections)
                                    id++
                                }
                                for (xyz in 0 ..< 16 * 16 * 15)
                                    occlude[id++] = true
                            } else {
                                id += 16 * 16 * 16
                            }
                        }

                        is ListPalette, is MapPalette -> {
                            val storage = section.chunkData.storage!!
                            val isOcclude = BooleanArray(palette.size()) { id -> palette.idToState(id).occlude }
                            forChunk2D { x, z ->
                                if (isOcclude[storage.get(id and 0xfff)])
                                    handleOccludePrevSection(occlude, invisible, index, x, z, id, sections)
                                id++
                            }
                            for (y in 0 ..< 15) {
                                forChunk2D { x, z ->
                                    if (isOcclude[storage.get(id and 0xfff)])
                                        handleOcclude(occlude, invisible, x, z, id, storage)
                                    id++
                                }
                            }
                        }

                        else -> {
                            val storage = section.chunkData.storage!!
                            forChunk2D { x, z ->
                                if (palette.idToState(storage.get(id and 0xfff)).occlude)
                                    handleOccludePrevSection(occlude, invisible, x, index, z, id, sections)
                                id++
                            }
                            for (y in 0 ..< 15) {
                                forChunk2D { x, z ->
                                    if (palette.idToState(storage.get(id and 0xfff)).occlude)
                                        handleOcclude(occlude, invisible, x, z, id, storage)
                                    id++
                                }
                            }
                        }

                    }
                }
                counter.minimalChunks++
                miniChunks[chunkKey] = invisible.clone()
//                println("TIME: ${System.nanoTime() - tm}")
            }
        }
    }

    private inline fun forChunk2D(crossinline scope: (x: Int, z: Int) -> Unit) {
        for (x in 0 ..< 16)
            for (z in 0 ..< 16)
                scope(x, z)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun handleOccludePrevSection(occlude: BooleanArray, invisible: BooleanArray, index: Int,
                                                x: Int, z: Int, id: Int, sections: Array<BaseChunk>) {
        occlude[id] = true
        if (x >= 2 && index != 0 && z >= 2 // We want to keep the edge so the algo is simpler
            // Check if the block is invisible
            && occlude[id - 0x211] && occlude[id - 0x121] && occlude[id - 0x112]
            && occlude[id - 0x011] && occlude[id - 0x110] && occlude[id - 0x101]) {

            val last = (sections[index - 1] as Chunk_v1_18).chunkData.storage
            if (last != null) {
                last.set(id - 0x111 and 0xfff, 0)
                invisible[id - 0x111] = true
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun handleOcclude(occlude: BooleanArray, invisible: BooleanArray,
                                     x: Int, z: Int, id: Int, storage: BaseStorage) {
        occlude[id] = true
        if (x >= 2 && id >= 0x200 && z >= 2
            && occlude[id - 0x211] && occlude[id - 0x121] && occlude[id - 0x112]
            && occlude[id - 0x011] && occlude[id - 0x110] && occlude[id - 0x101]) {

            storage.set(id - 0x111 and 0xfff, 0)
            invisible[id - 0x111] = true
        }
    }

    private fun checkBlockUpdate(player: Player,
                                 blockLocation: Vector3i, minHeight: Int = player.world.minHeight): Boolean {
        return checkBlockUpdate(player, blockLocation.x, blockLocation.y, blockLocation.z, minHeight)
    }

    private fun checkBlockUpdate(player: Player,
                                 x: Int, y: Int, z: Int, minHeight: Int = player.world.minHeight): Boolean {
        val miniChunks = player.miniChunks
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val chunkKey = Chunk.getChunkKey(chunkX, chunkZ)
        val invisible = miniChunks[chunkKey] ?: return true
        if (invisible === FULL_CHUNK) return true

        val id = blockKeyChunkAnd(x, y, z, minHeight)
        return checkBlockUpdate1(player, miniChunks, invisible, chunkX, chunkZ, chunkKey, id)
    }

    private fun checkBlockUpdate(player: Player, chunkX: Int, chunkZ: Int,
                                 x: Int, y: Int, z: Int, minHeight: Int = player.world.minHeight): Boolean {
        val miniChunks = player.miniChunks
        val chunkKey = Chunk.getChunkKey(chunkX, chunkZ)
        val invisible = miniChunks[chunkKey] ?: return true
        if (invisible === FULL_CHUNK) return true

        val id = blockKeyChunk(x, y, z, minHeight)
        return checkBlockUpdate1(player, miniChunks, invisible, chunkX, chunkZ, chunkKey, id)
    }

    private fun checkBlockUpdate1(player: Player, miniChunks: Long2ObjectMap<BooleanArray>, invisible: BooleanArray,
                                  chunkX: Int, chunkZ: Int, chunkKey: Long, blockId: Int): Boolean {
        if (blockId == -1) {
            // Overflows, we just return
            return false
        }
        var needsUpdate = false
        blockId.nearbyBlockId(invisible.size shr 8) { blockId ->
            if (needsUpdate || invisible[blockId]) {
                needsUpdate = true
            }
        }
        if (needsUpdate) {
            miniChunks[chunkKey] = FULL_CHUNK
            try {
                val nms = (player as CraftPlayer).handle
                val level = nms.serverLevel()
                PlayerChunkSender.sendChunk(nms.connection, level, level.getChunkIfLoaded(chunkX, chunkZ)!!)
                counter.resentChunks++
            } catch (e: Exception) {
                miniChunks[chunkKey] = invisible
                throw e
            }
            return true
        }
        return false
    }

    private data class ChunkPos(val x: Int, val z: Int)
    private val Long.chunkPos
        get() = ChunkPos((this and 0xffffffffL).toInt(), (this shr 32).toInt())

    private fun blockKeyChunk(x: Int, y: Int, z: Int, minHeight: Int): Int {
        return x or (z shl 4) or (y - minHeight shl 8)
    }
    private fun blockKeyChunkAnd(x: Int, y: Int, z: Int, minHeight: Int): Int {
        return (x and 0xf) or (z and 0xf shl 4) or (y - minHeight shl 8)
    }
    private val Int.chunkBlockPos: ChunkBlockPos
        get() = ChunkBlockPos(this and 0xf, this shr 8, this shr 4 and 0xf)
    private data class ChunkBlockPos(val x: Int, val y: Int, val z: Int)
    private inline fun Int.nearbyBlockId(height: Int, crossinline scope: (Int) -> Unit) {
        val (x, y, z) = this.chunkBlockPos
        if (x > 0)          scope(this - 0x001)
        if (x < 16)         scope(this + 0x001)
        if (z > 0)          scope(this - 0x010)
        if (z < 16)         scope(this + 0x010)
        if (y > 0)          scope(this - 0x100)
        if (y < height - 1) scope(this + 0x100)
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

    class OccludeCache(
        val cached: BooleanArray = BooleanArray(1 shl 16),
        val value: BooleanArray = BooleanArray(1 shl 16),
    )

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
    )
}