package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.stream.NetStreamInput
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18
import com.github.retrooper.packetevents.protocol.world.chunk.palette.GlobalPalette
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
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.PlayerChunkSender
import net.minecraft.util.BitStorage
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.SingleValuePalette
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@Suppress("NOTHING_TO_INLINE")
object ChunkDataThrottle: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    private val FULL_CHUNK = BooleanArray(0)

    private val minimalChunks = hashMapOf<Player, Long2ObjectMap<BooleanArray>>()
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
                    if (player.isChunkSent(chunkKey))
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
        // Clear these so we can save our memory
        minimalChunks.values.forEach { it.clear() }
        minimalChunks.clear()
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        e.player.miniChunks
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        minimalChunks.remove(e.player)?.clear()
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

                val nms = player.nms
                val level = nms.serverLevel()

                val sections = column.chunks
                val height = event.user.totalWorldHeight
                val occlude = BooleanArray(height shl 8)
                // Handle neighbour chunks starts
                val occludeNeighbor = BooleanArray(4 * 16 * (height + 1))

                handleNeighbourChunk(occludeNeighbor, level, 0, column.x - 1, column.z, 0x0f, 0x10)
                handleNeighbourChunk(occludeNeighbor, level, 1, column.x + 1, column.z, 0x00, 0x10)
                handleNeighbourChunk(occludeNeighbor, level, 2, column.x, column.z - 1, 0xf0, 0x01)
                handleNeighbourChunk(occludeNeighbor, level, 3, column.x, column.z + 1, 0x00, 0x01)
                // Handle neighbour chunks ends
                val invisible = BooleanArray(height shl 8)

                var allInvisible = false
                var id = 0
                out@ for ((index, section) in sections.withIndex()) {
                    section as Chunk_v1_18
                    val palette = section.chunkData.palette
                    when (palette) {
                        is SingletonPalette -> {
                            // This section contains only one block type, and it's already the smallest way.
                            if (palette.idToState(0).occlude) {
                                forChunk2D { x, z ->
                                    if (!handleOccludePrevSection(occlude, occludeNeighbor, invisible, index, x, z, id, sections))
                                        allInvisible = false
                                    id++
                                }
                                if (allInvisible) (sections[index - 1] as Chunk_v1_18).chunkData.palette = CustomSingletonPalette(0)
                                allInvisible = true

                                for (xyz in 0 until 16 * 16 * 15)
                                    occlude[id++] = true
                            } else {
                                id += 16 * 16 * 16
                                allInvisible = false
                            }
                        }
                        is ListPalette, is MapPalette -> {
                            val storage = section.chunkData.storage!!
                            val isOcclude = BooleanArray(palette.size()) { id -> palette.idToState(id).occlude }
                            forChunk2D { x, z ->
                                if (!isOcclude[storage.get(id and 0xfff)] ||
                                    !handleOccludePrevSection(occlude, occludeNeighbor, invisible, index, x, z, id, sections))
                                    allInvisible = false
                                id++
                            }
                            if (allInvisible) (sections[index - 1] as Chunk_v1_18).chunkData.palette = CustomSingletonPalette(0)
                            allInvisible = true

                            for (y in 0 until 15) forChunk2D { x, z ->
                                if (!isOcclude[storage.get(id and 0xfff)] ||
                                    !handleOcclude(occlude, occludeNeighbor, invisible, x, z, id, storage))
                                    allInvisible = false
                                id++
                            }
                        }
                        is GlobalPalette -> {
                            val storage = section.chunkData.storage!!
                            forChunk2D { x, z ->
                                if (!storage.get(id and 0xfff).occlude ||
                                    !handleOccludePrevSection(occlude, occludeNeighbor, invisible, index, x, z, id, sections))
                                    allInvisible = false
                                id++
                            }
                            if (allInvisible) (sections[index - 1] as Chunk_v1_18).chunkData.palette = CustomSingletonPalette(0)
                            allInvisible = true

                            for (y in 0 until 15) forChunk2D { x, z ->
                                if (!(storage.get(id and 0xfff).occlude ||
                                            !handleOcclude(occlude, occludeNeighbor, invisible, x, z, id, storage)))
                                    allInvisible = false
                                id++
                            }
                        }
                        else -> {
                            throw AssertionError("Unsupported packetevents palette type: ${palette::class.simpleName}")
                        }
                    }
                }
                // We don't check allInvisible for last section. It never happens in vanilla generated chunks.
                counter.minimalChunks++
                miniChunks[chunkKey] = invisible
//                println("TIME: ${System.nanoTime() - tm}ns")
            }
        }
    }

    private inline fun forChunk2D(crossinline scope: (x: Int, z: Int) -> Unit) {
        for (z in 0 until 16)
            for (x in 0 until 16)
                scope(x, z)
    }

    val palettedContainerDataClass = PalettedContainer::class.java.getDeclaredField("data").type
    val paletteField = palettedContainerDataClass.getDeclaredField("palette").also { it.isAccessible = true }
    val storageField = palettedContainerDataClass.getDeclaredField("storage").also { it.isAccessible = true }

    private inline fun handleNeighbourChunk(occludeNeighbor: BooleanArray, level: ServerLevel,
                                            iType: Int, chunkX: Int, chunkZ: Int, bid: Int, bidStep: Int) {
        level.getChunkIfLoaded(chunkX, chunkZ)?.let { chunk ->
            val indexLoop = 0x100 - bidStep * 16
            var blockId = bid
            var i = 4 + iType shl 4
            for (section in chunk.sections) {
                val data = section.states.data
                @Suppress("UNCHECKED_CAST")
                val palette = paletteField[data] as Palette<BlockState>
                when (palette) {
                    is SingleValuePalette<BlockState> -> {
                        if (palette.valueFor(0).occlude) {
                            for (y in 0 until 16) {
                                for (j in 0 until 16) {
                                    occludeNeighbor[i++] = true
                                    blockId += bidStep
                                }
                                blockId += indexLoop
                                i = i + (0b0001_00_0000 - 0b1_0000)
                            }
                        } else {
                            blockId += 16 * 16 * 16
                            i += 0b0001_00_0000 * 16
                        }
                    }
                    is LinearPalette<BlockState>, is HashMapPalette<BlockState> -> {
                        val storage = storageField[data] as BitStorage
                        val isOcclude = BooleanArray(palette.size) { id -> palette.valueFor(id).occlude }
                        for (y in 0 until 16) {
                            for (j in 0 until 16) {
                                if (isOcclude[storage.get(blockId and 0xfff)])
                                    occludeNeighbor[i] = true
                                i++
                                blockId += bidStep
                            }
                            blockId += indexLoop
                            i = i + (0b0001_00_0000 - 0b1_0000)
                        }
                    }
                    is net.minecraft.world.level.chunk.GlobalPalette<BlockState> -> {
                        val storage = storageField[data] as BitStorage
                        for (y in 0 until 16) {
                            for (j in 0 until 16) {
                                if (storage.get(blockId and 0xfff).occlude)
                                    occludeNeighbor[i] = true
                                i++
                                blockId += bidStep
                            }
                            blockId += indexLoop
                            i = i + (0b0001_00_0000 - 0b1_0000)
                        }
                    }
                    else -> {
                        throw AssertionError("Unsupported minecraft palette type: ${palette::class.simpleName}")
                    }
                }
            }
        }
    }

    private inline fun handleOccludePrevSection(occlude: BooleanArray, occludeNeighbor: BooleanArray, invisible: BooleanArray,
                                                index: Int, x: Int, z: Int, id: Int, sections: Array<BaseChunk>): Boolean {
        occlude[id] = true
        if (index == 0)
            return true

        if (isInvisible(occlude, occludeNeighbor, id, x, z)) {
            val chunkData = (sections[index - 1] as Chunk_v1_18).chunkData
            val last = chunkData.storage
            if (last != null) {
                last.set(id - 0x100 and 0xfff, 0)
                invisible[id - 0x100] = true
            }
            // If the block in last section is not invisible it should be already false so we don't do a check here.
            return true
        }
        return false
    }

    private inline fun handleOcclude(occlude: BooleanArray, occludeNeighbor: BooleanArray, invisible: BooleanArray,
                                     x: Int, z: Int, id: Int, storage: BaseStorage): Boolean {
        occlude[id] = true
        if (isInvisible(occlude, occludeNeighbor, id, x, z)) {
            storage.set(id - 0x100 and 0xfff, 0)
            invisible[id - 0x100] = true
            return true
        }
        return false
    }

    private inline fun isInvisible(occlude: BooleanArray, occludeNeighbor: BooleanArray,
                                   id: Int, x: Int, z: Int): Boolean {
        return (id < 0x200 || occlude[id - 0x200])
                && (if (x == 0 ) occludeNeighbor[(0 shl 4) + (id shr 8 shl 2 + 4) + z] else occlude[id - (0x101)])
                && (if (z == 0 ) occludeNeighbor[(2 shl 4) + (id shr 8 shl 2 + 4) + x] else occlude[id - (0x110)])
                && (if (x == 15) occludeNeighbor[(1 shl 4) + (id shr 8 shl 2 + 4) + z] else occlude[id - (0x100 - 0x01)])
                && (if (z == 15) occludeNeighbor[(3 shl 4) + (id shr 8 shl 2 + 4) + x] else occlude[id - (0x100 - 0x10)])
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
        return true
        // TODO
        var needsUpdate = false
        blockId.nearbyBlockId(invisible.size shr 8) { blockId ->
            if (needsUpdate || invisible.size > blockId && invisible[blockId]) {
                needsUpdate = true
            }
        }
        if (needsUpdate) {
            miniChunks[chunkKey] = FULL_CHUNK
            try {
                val nms = player.nms
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

    private val Player.nms
        get() = (player as CraftPlayer).handle

    private val BlockState.id
        get() = Block.BLOCK_STATE_REGISTRY.getId(this)

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

    private inline val Int.occlude
        get() = if (occludeCache.cached[this]) {
            occludeCache.value[this]
        } else {
            cacheOcclude(this)
        }

    private inline val BlockState.occlude: Boolean
        get() = Block.BLOCK_STATE_REGISTRY.getId(this).occlude

    private inline fun cacheOcclude(blockId: Int): Boolean {
        val wrapped = WrappedBlockState.getByGlobalId(PacketEvents.getAPI().serverManager.version.toClientVersion(), blockId, false)
        val material = SpigotConversionUtil.toBukkitBlockData(wrapped).material
        return material.isOccluding.also {
            occludeCache.cached[blockId] = true
            occludeCache.value[blockId] = it
        }
    }

    class OccludeCache(
        val cached: BooleanArray = BooleanArray(Block.BLOCK_STATE_REGISTRY.size()),
        val value: BooleanArray = BooleanArray(Block.BLOCK_STATE_REGISTRY.size()),
    )

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
    )

    private class CustomSingletonPalette(state: Int): SingletonPalette(CustomNetStreamInput(state))

    private class CustomNetStreamInput(val value: Int): NetStreamInput(null) {

        override fun readVarInt(): Int {
            return value
        }
    }
}