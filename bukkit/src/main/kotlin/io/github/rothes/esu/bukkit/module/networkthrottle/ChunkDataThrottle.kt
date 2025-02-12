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
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import it.unimi.dsi.fastutil.shorts.ShortList
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
import org.bukkit.World.Environment
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.reflect.Field

@Suppress("NOTHING_TO_INLINE")
object ChunkDataThrottle: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    private val FULL_CHUNK = BooleanArray(0)

    private val minimalChunks = hashMapOf<Player, Long2ObjectMap<BooleanArray>>()
    private val blockingCache = PacketEvents.getAPI().serverManager.version.toClientVersion().let { version ->
        BooleanArray(Block.BLOCK_STATE_REGISTRY.size()) { id ->
            val wrapped = WrappedBlockState.getByGlobalId(version, id, false)
            val material = SpigotConversionUtil.toBukkitBlockData(wrapped).material
            material.isOccluding
        }
    }
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
        val config = config.chunkDataThrottle
        if (!config.enabled) {
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
                    if (block.blockId.blocking) {
                        // Only check full chunk if blocks get broken or transformed to non-blocking
                        continue
                    }
                    checkBlockUpdate(player, block.x, block.y, block.z, minHeight)
                }
            }
            PacketType.Play.Server.BLOCK_CHANGE -> {
                val wrapper = WrapperPlayServerBlockChange(event)
                if (wrapper.blockId.blocking) {
                    // Only check full chunk if blocks get broken or transformed to non-blocking
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
                val recreatePaletteMappings = config.recreatePaletteMappings
                val minimalHeightInvisibleCheck = config.minimalHeightInvisibleCheck
                val singleValuedSectionBlockIds = config.singleValuedSectionBlockIds.getOrDefault(level.serverLevelData.levelName)!!

                val sections = column.chunks
                val height = event.user.totalWorldHeight
                val blocking = BooleanArray(height shl 8)
                // Handle neighbour chunks starts
                val blockingNeighbor = BooleanArray(4 * 16 * (height + 1))

                handleNeighbourChunk(blockingNeighbor, level, 0, column.x - 1, column.z, 0x0f, 0x10)
                handleNeighbourChunk(blockingNeighbor, level, 1, column.x + 1, column.z, 0x00, 0x10)
                handleNeighbourChunk(blockingNeighbor, level, 2, column.x, column.z - 1, 0xf0, 0x01)
                handleNeighbourChunk(blockingNeighbor, level, 3, column.x, column.z + 1, 0x00, 0x01)
                // Handle neighbour chunks ends
                val invisible = BooleanArray(height shl 8)

                var allInvisible = false
                var id = 0
                out@ for ((index, section) in sections.withIndex()) {
                    if (index == 8 && allInvisible && !config.netherRoofInvisibleCheck && level.world.environment == Environment.NETHER)
                        (sections[index - 1] as Chunk_v1_18).chunkData.palette = CustomSingletonPalette(singleValuedSectionBlockIds.random())

                    section as Chunk_v1_18
                    val palette = section.chunkData.palette
                    when (palette) {
                        is SingletonPalette -> {
                            // This section contains only one block type, and it's already the smallest way.
                            if (palette.idToState(0).blocking) {
                                forChunk2D { x, z ->
                                    if (!handleBlockingPrevSec(blocking, blockingNeighbor, invisible, minimalHeightInvisibleCheck, index, x, z, id, sections))
                                        allInvisible = false
                                    id++
                                }
                                if (allInvisible) (sections[index - 1] as Chunk_v1_18).chunkData.palette = CustomSingletonPalette(singleValuedSectionBlockIds.random())
                                allInvisible = true

                                for (xyz in 0 until 16 * 16 * 15)
                                    blocking[id++] = true
                            } else {
                                id += 16 * 16 * 16
                                allInvisible = false
                            }
                        }
                        is ListPalette, is MapPalette -> {
                            val storage = section.chunkData.storage!!
                            val blockingArr = if (recreatePaletteMappings) {
                                val arr = Array<BlockType>(palette.size()) { i -> BlockType(palette.idToState(i), i) }
                                for (i in 0 until 16 * 16 * 16) arr[storage.get(i)].blocks.add(i.toShort())
                                arr.sortByDescending { it.blocks.size }
                                for ((i, data) in arr.withIndex()) {
                                    if (data.oldMapId != i) {
                                        val iterator = data.blocks.iterator()
                                        while (iterator.hasNext())
                                            storage.set(iterator.nextShort().toInt(), i)
                                    }
                                }
                                section.chunkData.palette = CustomListPalette(if (palette is ListPalette) 4 else 8, arr)
                                BooleanArray(palette.size()) { id -> arr[id].blockId.blocking }
                            } else {
                                BooleanArray(palette.size()) { id -> palette.idToState(id).blocking }
                            }
                            forChunk2D { x, z ->
                                if (!blockingArr[storage.get(id and 0xfff)] ||
                                    !handleBlockingPrevSec(blocking, blockingNeighbor, invisible, minimalHeightInvisibleCheck, index, x, z, id, sections))
                                    allInvisible = false
                                id++
                            }
                            if (allInvisible) (sections[index - 1] as Chunk_v1_18).chunkData.palette = CustomSingletonPalette(singleValuedSectionBlockIds.random())
                            allInvisible = true

                            for (y in 0 until 15) forChunk2D { x, z ->
                                if (!blockingArr[storage.get(id and 0xfff)] ||
                                    !handleBlocking(blocking, blockingNeighbor, invisible, minimalHeightInvisibleCheck, x, z, id, storage))
                                    allInvisible = false
                                id++
                            }
                        }
                        is GlobalPalette -> {
                            val storage = section.chunkData.storage!!
                            forChunk2D { x, z ->
                                if (!storage.get(id and 0xfff).blocking ||
                                    !handleBlockingPrevSec(blocking, blockingNeighbor, invisible, minimalHeightInvisibleCheck, index, x, z, id, sections))
                                    allInvisible = false
                                id++
                            }
                            if (allInvisible) (sections[index - 1] as Chunk_v1_18).chunkData.palette = CustomSingletonPalette(singleValuedSectionBlockIds.random())
                            allInvisible = true

                            for (y in 0 until 15) forChunk2D { x, z ->
                                if (!(storage.get(id and 0xfff).blocking ||
                                            !handleBlocking(blocking, blockingNeighbor, invisible, minimalHeightInvisibleCheck, x, z, id, storage)))
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

    private data class BlockType(val blockId: Int, val oldMapId: Int, val blocks: ShortList = ShortArrayList(8))

    private inline fun forChunk2D(crossinline scope: (x: Int, z: Int) -> Unit) {
        for (z in 0 until 16)
            for (x in 0 until 16)
                scope(x, z)
    }

    val pcDataClass: Class<*> = PalettedContainer::class.java.getDeclaredField("data").type
    val paletteField: Field = pcDataClass.getDeclaredField("palette").also { it.isAccessible = true }
    val storageField: Field = pcDataClass.getDeclaredField("storage").also { it.isAccessible = true }

    private inline fun handleNeighbourChunk(blockingNeighbor: BooleanArray, level: ServerLevel,
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
                        if (palette.valueFor(0).blocking) {
                            for (y in 0 until 16) {
                                for (j in 0 until 16) {
                                    blockingNeighbor[i++] = true
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
                        val blockingArr = BooleanArray(palette.size) { id -> palette.valueFor(id).blocking }
                        for (y in 0 until 16) {
                            for (j in 0 until 16) {
                                if (blockingArr[storage.get(blockId and 0xfff)])
                                    blockingNeighbor[i] = true
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
                                if (storage.get(blockId and 0xfff).blocking)
                                    blockingNeighbor[i] = true
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

    private inline fun handleBlockingPrevSec(blocking: BooleanArray, blockingNeighbor: BooleanArray, invisible: BooleanArray, minimalHeightInvisibleCheck: Boolean,
                                             index: Int, x: Int, z: Int, id: Int, sections: Array<BaseChunk>): Boolean {
        blocking[id] = true
        if (index == 0)
            return true

        if (isInvisible(blocking, blockingNeighbor, minimalHeightInvisibleCheck, id, x, z)) {
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

    private inline fun handleBlocking(blocking: BooleanArray, blockingNeighbor: BooleanArray, invisible: BooleanArray, minimalHeightInvisibleCheck: Boolean,
                                      x: Int, z: Int, id: Int, storage: BaseStorage): Boolean {
        blocking[id] = true
        if (isInvisible(blocking, blockingNeighbor, minimalHeightInvisibleCheck, id, x, z)) {
            storage.set(id - 0x100 and 0xfff, 0)
            invisible[id - 0x100] = true
            return true
        }
        return false
    }

    private inline fun isInvisible(blocking: BooleanArray, blockingNeighbor: BooleanArray, minimalHeightInvisibleCheck: Boolean,
                                   id: Int, x: Int, z: Int): Boolean {
        return (    if (id < 0x200) !minimalHeightInvisibleCheck                       else blocking[id - (0x200)])
                && (if (x == 0 ) blockingNeighbor[(0 shl 4) + (id shr 8 shl 2 + 4) + z] else blocking[id - (0x101)])
                && (if (z == 0 ) blockingNeighbor[(2 shl 4) + (id shr 8 shl 2 + 4) + x] else blocking[id - (0x110)])
                && (if (x == 15) blockingNeighbor[(1 shl 4) + (id shr 8 shl 2 + 4) + z] else blocking[id - (0x100 - 0x01)])
                && (if (z == 15) blockingNeighbor[(3 shl 4) + (id shr 8 shl 2 + 4) + x] else blocking[id - (0x100 - 0x10)])
    }

    private fun checkBlockUpdate(player: Player, blockLocation: Vector3i, minHeight: Int = player.world.minHeight) {
        return checkBlockUpdate(player, blockLocation.x, blockLocation.y, blockLocation.z, minHeight)
    }

    private fun checkBlockUpdate(player: Player, x: Int, y: Int, z: Int, minHeight: Int = player.world.minHeight) {
        val map = player.miniChunks
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val chunkKey = Chunk.getChunkKey(chunkX, chunkZ)

        val bid = blockKeyChunkAnd(x, y, z, minHeight)
        val nms = player.nms
        val level = nms.serverLevel()
        fun handleRelativeChunk(chunkOff: Long, blockOff: Int, xOff: Int, zOff: Int) {
            val invisibleNearby = map[chunkKey + chunkOff] ?: return
            if (bid > invisibleNearby.size) return // Well, on highest Y? I'm not sure how it happens.
            if (invisibleNearby != FULL_CHUNK && invisibleNearby[bid + blockOff]) {
                map[chunkKey + chunkOff] = FULL_CHUNK
                try {
                    val chunk = level.getChunkIfLoaded(chunkX + xOff, chunkZ + zOff) ?: error("Failed to resent chunk (${chunkX + xOff}, ${chunkZ + zOff}) for ${player.name}, it is not loaded!")
                    PlayerChunkSender.sendChunk(nms.connection, level, chunk)
                    counter.resentChunks++
                } catch (e: Exception) {
                    map[chunkKey] = invisibleNearby
                    throw e
                }
            }
        }

        val (x, y, z) = bid.chunkBlockPos
        if (x == 0 ) handleRelativeChunk(-0x000000001, +15 shl 0, -1, +0)
        if (x == 15) handleRelativeChunk(+0x000000001, -15 shl 0, +1, +0)
        if (z == 0 ) handleRelativeChunk(-0x100000000, +15 shl 4, +0, -1)
        if (z == 15) handleRelativeChunk(+0x100000000, -15 shl 4, +0, +1)

        val invisible = map[chunkKey] ?: return
        if (invisible === FULL_CHUNK) return

        val height = invisible.size shr 8
        if (y >= height) return

        val update = (if (x > 0 )         invisible[bid - 0x001] else false) ||
                     (if (x < 15)         invisible[bid + 0x001] else false) ||
                     (if (z > 0 )         invisible[bid - 0x010] else false) ||
                     (if (z < 15)         invisible[bid + 0x010] else false) ||
                     (if (y > 0 )         invisible[bid - 0x100] else false) ||
                     (if (y < height - 1) invisible[bid + 0x100] else false)
        if (update) {
            map[chunkKey] = FULL_CHUNK
            try {
                val nms = player.nms
                val level = nms.serverLevel()
                val chunk = level.getChunkIfLoaded(chunkX, chunkZ) ?: error("Failed to resent chunk ($chunkX, $chunkZ) for ${player.name}, it is not loaded!")
                PlayerChunkSender.sendChunk(nms.connection, level, chunk)
                counter.resentChunks++
            } catch (e: Exception) {
                map[chunkKey] = invisible
                throw e
            }
        }
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
    private inline fun Int.nearbyBlockVisible(map: Long2ObjectMap<BooleanArray>, invisible: BooleanArray,
                                              chunk: Long): Boolean {
        val height = invisible.size shr 8
        println("${chunk.chunkPos}\n" +
                "${(chunk - 0x000000001).chunkPos} = ${map[chunk - 0x000000001]?.get(this + 15)        }\n" +
                "${(chunk + 0x000000001).chunkPos} = ${map[chunk + 0x000000001]?.get(this - 15)        }\n" +
                "${(chunk - 0x100000000).chunkPos} = ${map[chunk - 0x100000000]?.get(this + (15 shl 4))}\n" +
                "${(chunk + 0x100000000).chunkPos} = ${map[chunk + 0x100000000]?.get(this - (15 shl 4))}\n")
        val (x, y, z) = this.chunkBlockPos
        return (if (x == 0 ) map[chunk - 0x000000001]?.get(this + 15)         ?: false else invisible[this - 0x001]) ||
               (if (x == 15) map[chunk + 0x000000001]?.get(this - 15)         ?: false else invisible[this + 0x001]) ||
               (if (z == 0 ) map[chunk - 0x100000000]?.get(this + (15 shl 4)) ?: false else invisible[this - 0x010]) ||
               (if (z == 15) map[chunk + 0x100000000]?.get(this - (15 shl 4)) ?: false else invisible[this + 0x010]) ||
               (if (y > 0 )         invisible[this - 0x100] else false) ||
               (if (y < height - 1) invisible[this + 0x100] else false)
    }

    private inline val Int.blocking
        get() = blockingCache[this]

    private inline val BlockState.blocking: Boolean
        get() = Block.BLOCK_STATE_REGISTRY.getId(this).blocking

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
    )

    private class CustomListPalette(bits: Int, array: Array<BlockType>): ListPalette(bits, CustomNetStreamInput(array)) {

        private class CustomNetStreamInput(val array: Array<BlockType>) : NetStreamInput(null) {
            private var read = -2
            override fun readVarInt(): Int {
                return if (++read == -1) array.size else array[read].blockId
            }
        }
    }

    private class CustomSingletonPalette(state: Int): SingletonPalette(CustomNetStreamInput(state)) {

        private class CustomNetStreamInput(val value: Int) : NetStreamInput(null) {
            override fun readVarInt(): Int {
                return value
            }
        }
    }
}