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
import com.google.common.io.ByteStreams
import com.google.common.primitives.Ints
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.plugin
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import it.unimi.dsi.fastutil.shorts.ShortList
import net.jpountz.lz4.LZ4Factory
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
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
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.experimental.or
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream
import kotlin.math.abs
import kotlin.time.Duration.Companion.nanoseconds

@Suppress("NOTHING_TO_INLINE")
object ChunkDataThrottle: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    // The block B is in center. if Y_MINUS, block in B's bottom is occluding(i.e. blocking) .
    private const val X_PLUS    = 0b00001.toByte()
    private const val X_MINUS   = 0b00010.toByte()
    private const val Z_PLUS    = 0b00100.toByte()
    private const val Z_MINUS   = 0b01000.toByte()
    private const val Y_MINUS   = 0b10000.toByte()
    private const val INVISIBLE = 0b11111.toByte()

    init {
        if (Block.BLOCK_STATE_REGISTRY.size() > Short.MAX_VALUE)
            error("Block states has exceeded max value of short! This is not supported. " +
                    "${Block.BLOCK_STATE_REGISTRY.size()} > ${Short.MAX_VALUE}")
    }

    private val FULL_CHUNK = PlayerChunk(BitSet(0))

    private val hotDataFile = NetworkThrottleModule.moduleFolder.resolve("minimalChunksData.tmp")
    private val minimalChunks = hashMapOf<Player, Long2ObjectMap<PlayerChunk>>()
    private val blockingCache = PacketEvents.getAPI().serverManager.version.toClientVersion().let { version ->
        BooleanArray(Block.BLOCK_STATE_REGISTRY.size()) { id ->
            val wrapped = WrappedBlockState.getByGlobalId(version, id, false)
            val material = SpigotConversionUtil.toBukkitBlockData(wrapped).material
            material.isOccluding
        }
    }
    val counter = Counter()

    fun onEnable() {
        try {
            val toFile = hotDataFile.toFile()
            if (toFile.exists()) {
                val nanoTime = System.nanoTime()
                val readBytes = toFile.readBytes()
                val uncompressedSize = Ints.fromByteArray(readBytes.copyOf(4))
                val input = ByteStreams.newDataInput(
                    LZ4Factory.fastestInstance().fastDecompressor().decompress(readBytes, 4, uncompressedSize)
                )
                val players = input.readInt()
                for (i in 0 until players) {
                    val uuidString = String(ByteArray(36) { input.readByte() }, Charsets.US_ASCII)
                    val uuid = UUID.fromString(uuidString)
                    val mapSize = input.readInt()
                    val player = Bukkit.getPlayer(uuid)
                    if (player == null) {
                        for (j in 0 until mapSize) {
                            input.readLong()
                            for (k in 0 until input.readInt())
                                input.readLong()
                        }
                        continue
                    }

                    val miniChunks = player.miniChunks
                    for (j in 0 until mapSize) {
                        val chunkKey = input.readLong()
                        val longArraySize = input.readInt()
                        if (!player.isChunkSent(chunkKey)) {
                            input.skipBytes(longArraySize * 8)
                            continue
                        }
                        val longArray = LongArray(longArraySize) { input.readLong() }
                        miniChunks[chunkKey] = PlayerChunk(BitSet.valueOf(longArray))
                    }
                }
                toFile.delete()
                plugin.info("Loaded ChunkDataThrottle hotData in ${(System.nanoTime() - nanoTime).nanoseconds}")
            }
        } catch (e: Exception) {
            plugin.err("Failed to load hotData", e)
        }
        PacketEvents.getAPI().eventManager.registerListener(this)
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun onDisable() {
        PacketEvents.getAPI().eventManager.unregisterListener(this)
        HandlerList.unregisterAll(this)

        if (plugin.isEnabled || plugin.disabledHot) {
            val nanoTime = System.nanoTime()
            var bufferSize = 4 // Map size
            val filter = minimalChunks.entries.filter { it.value.isNotEmpty() }
            for ((_, map) in filter) {
                bufferSize += 36 // UUID
                bufferSize += 4 // PlayerChunk Map size
                bufferSize += map.size * ((1 + 1472) * 8) // ChunkKey and Long Arrays
            }
            val output = ByteStreams.newDataOutput(bufferSize)

            output.writeInt(filter.size)
            for ((user, map) in filter) {
                output.write(user.uniqueId.toString().toByteArray(Charsets.US_ASCII))
                val map = map.long2ObjectEntrySet().filter { it.value !== FULL_CHUNK }
                output.writeInt(map.size)
                if (map.isEmpty()) {
                    continue
                }
                for ((chunkKey, playerChunk) in map) {
                    output.writeLong(chunkKey)
                    val longs = playerChunk.invisible.toLongArray()
                    output.writeInt(longs.size)
                    for (long in longs) {
                        output.writeLong(long)
                    }
                }
            }
            hotDataFile.outputStream(StandardOpenOption.CREATE).use {
                val data = output.toByteArray()
                it.write(Ints.toByteArray(data.size))
                it.write(LZ4Factory.fastestInstance().fastCompressor().compress(data))
                it.flush()
            }
            plugin.info("Saved ChunkDataThrottle hotData in ${(System.nanoTime() - nanoTime).nanoseconds}. Size ${hotDataFile.fileSize()}")
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
        if (event.isCancelled) return
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
        if (event.isCancelled) return
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
                val config = config.chunkDataThrottle
                if (!config.enabled) {
                    return
                }
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
                val rebuildPaletteMappings = config.rebuildPaletteMappings
                val minimalHeightInvisibleCheck = config.minimalHeightInvisibleCheck
                val randomBlockIds = config.singleValuedSectionBlockIds.getOrDefault(level.serverLevelData.levelName)!!

                val sections = column.chunks
                val height = event.user.totalWorldHeight
                val blocking = ByteArray((height shl 8) + 16*16)
                val isZero = BooleanArray(height shl 8)
                val invisible = BitSet(height shl 8)
                if (!minimalHeightInvisibleCheck)
                    for (i in 0 until 16 * 16)
                        blocking[i] = Y_MINUS
                // Handle neighbour chunks starts
                handleNeighbourChunk(blocking, level, column.x + 1, column.z, 0x00, 0x10, +0x0f, X_PLUS )
                handleNeighbourChunk(blocking, level, column.x - 1, column.z, 0x0f, 0x10, -0x0f, X_MINUS)
                handleNeighbourChunk(blocking, level, column.x, column.z + 1, 0x00, 0x01, +0xf0, Z_PLUS )
                handleNeighbourChunk(blocking, level, column.x, column.z - 1, 0xf0, 0x01, -0xf0, Z_MINUS)
                // Handle neighbour chunks ends

                var allInvisible = false
                var id = 0
                out@ for ((index, section) in sections.withIndex()) {
                    checkSectionAllInvisible(
                        allInvisible && index == 8 && !config.netherRoofInvisibleCheck
                                && level.world.environment == Environment.NETHER,
                        invisible, randomBlockIds, sections, index
                    )

                    section as Chunk_v1_18
                    val palette = section.chunkData.palette
                    when (palette) {
                        is SingletonPalette -> {
                            // This section contains only one block type, and it's already the smallest way.
                            // We don't check isZero here because we never write data to this.
                            if (palette.idToState(0).blocking) {
                                forChunk2D { x, z ->
                                    if (!handleBlockingPrevSec(blocking, isZero, invisible, index, x, z, id, sections))
                                        allInvisible = false
                                    id++
                                }
                                checkSectionAllInvisible(allInvisible, invisible, randomBlockIds, sections, index)
                                allInvisible = true

                                id += 16 * 16 * 15
                                for (i in id until id + 16 * 16)
                                    blocking[i] = blocking[i] or Y_MINUS
                            } else {
                                id += 16 * 16 * 16
                                allInvisible = false
                            }
                        }
                        is ListPalette, is MapPalette -> {
                            val storage = section.chunkData.storage!!
                            // rebuildPaletteMappings starts
                            /* TODO: Try to reduce the amount of block types; Each lower bit benefits at least 512 bytes.
                                 Approaches:
                                  a) Minecraft keeps the removed blocks in indexes, remove them;
                                  b) Convert all blocks in block types to id=0, if possible. As we sorted the types by amount, it's clear how to do that. */
                            val blockingArr = if (rebuildPaletteMappings) {
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
//                                val empty = arr.filter { it.blocks.isEmpty() }
//                                if (empty.isNotEmpty()) {
//                                    println("Block types: ${arr.size} - ${empty.size} not found; ${32 - arr.size.countLeadingZeroBits()} -> ${32 - (arr.size - empty.size).countLeadingZeroBits()}")
//                                }
                                section.chunkData.palette = CustomListPalette(if (palette is ListPalette) 4 else 8, arr)
                                BooleanArray(palette.size()) { id -> arr[id].blockId.blocking }
                            } else {
                                BooleanArray(palette.size()) { id -> palette.idToState(id).blocking }
                            }
                            // rebuildPaletteMappings ends
                            forChunk2D { x, z ->
                                val block = storage.get(id and 0xfff)
                                if (block == 0) isZero[id] = true
                                if (!blockingArr[block] ||
                                    !handleBlockingPrevSec(blocking, isZero, invisible, index, x, z, id, sections))
                                    allInvisible = false
                                id++
                            }
                            checkSectionAllInvisible(allInvisible, invisible, randomBlockIds, sections, index)
                            allInvisible = true

                            for (y in 0 until 15) forChunk2D { x, z ->
                                val block = storage.get(id and 0xfff)
                                if (block == 0) isZero[id] = true
                                if (!blockingArr[block] ||
                                    !handleBlocking(blocking, isZero, invisible, x, z, id, storage))
                                    allInvisible = false
                                id++
                            }
                        }
                        is GlobalPalette -> {
                            val storage = section.chunkData.storage!!
                            forChunk2D { x, z ->
                                val block = storage.get(id and 0xfff)
                                if (block == 0) isZero[id] = true
                                if (!block.blocking ||
                                    !handleBlockingPrevSec(blocking, isZero, invisible, index, x, z, id, sections))
                                    allInvisible = false
                                id++
                            }
                            checkSectionAllInvisible(allInvisible, invisible, randomBlockIds, sections, index)
                            allInvisible = true

                            for (y in 0 until 15) forChunk2D { x, z ->
                                val block = storage.get(id and 0xfff)
                                if (block == 0) isZero[id] = true
                                if (!(block.blocking ||
                                            !handleBlocking(blocking, isZero, invisible, x, z, id, storage)))
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
                miniChunks[chunkKey] = PlayerChunk(invisible)
//                val tim = System.nanoTime() - tm
//                if (tim < 600_000) timesl.add(tim)
//                if (timesl.size > 1000) println("AVG: ${timesl.drop(1000).average()}")
//                event.isCancelled = true
            }
        }
    }
//    private val timesl = LongArrayList(10000000)

    private data class BlockType(val blockId: Int, val oldMapId: Int, val blocks: ShortList = ShortArrayList(32))

    private inline fun forChunk2D(crossinline scope: (x: Int, z: Int) -> Unit) {
        for (z in 0 until 16)
            for (x in 0 until 16)
                scope(x, z)
    }

    val pcDataClass: Class<*> = PalettedContainer::class.java.getDeclaredField("data").type
    val paletteField: Field = pcDataClass.getDeclaredField("palette").also { it.isAccessible = true }
    val storageField: Field = pcDataClass.getDeclaredField("storage").also { it.isAccessible = true }

    private inline fun handleNeighbourChunk(blocking: ByteArray, level: ServerLevel, chunkX: Int, chunkZ: Int,
                                            bid: Int, bidStep: Int, arrOffset: Int, arrValue: Byte) {
        level.getChunkIfLoaded(chunkX, chunkZ)?.let { chunk ->
            val indexLoop = 0x100 - bidStep * 16
            var blockId = bid
            for (section in chunk.sections) {
                val data = section.states.data
                @Suppress("UNCHECKED_CAST")
                val palette = paletteField[data] as Palette<BlockState>
                when (palette) {
                    is SingleValuePalette<BlockState> -> {
                        if (palette.valueFor(0).blocking) {
                            for (y in 0 until 16) {
                                for (j in 0 until 16) {
                                    (blockId + arrOffset).let { blocking[it] = blocking[it] or arrValue }
                                    blockId += bidStep
                                }
                                blockId += indexLoop
                            }
                        } else {
                            blockId += 16 * 16 * 16
                        }
                    }
                    is LinearPalette<BlockState>, is HashMapPalette<BlockState> -> {
                        val storage = storageField[data] as BitStorage
                        val blockingArr = BooleanArray(palette.size) { id -> palette.valueFor(id).blocking }
                        for (y in 0 until 16) {
                            for (j in 0 until 16) {
                                if (blockingArr[storage.get(blockId and 0xfff)])
                                    (blockId + arrOffset).let { blocking[it] = blocking[it] or arrValue }
                                blockId += bidStep
                            }
                            blockId += indexLoop
                        }
                    }
                    is net.minecraft.world.level.chunk.GlobalPalette<BlockState> -> {
                        val storage = storageField[data] as BitStorage
                        for (y in 0 until 16) {
                            for (j in 0 until 16) {
                                if (storage.get(blockId and 0xfff).blocking)
                                    (blockId + arrOffset).let { blocking[it] = blocking[it] or arrValue }
                                blockId += bidStep
                            }
                            blockId += indexLoop
                        }
                    }
                    else -> {
                        throw AssertionError("Unsupported minecraft palette type: ${palette::class.simpleName}")
                    }
                }
            }
        }
    }

    private inline fun handleBlockingPrevSec(blocking: ByteArray, isZero: BooleanArray, invisible: BitSet,
                                             index: Int, x: Int, z: Int, id: Int, sections: Array<BaseChunk>): Boolean {
        addNearby(blocking, id, x, z)
        if (index == 0)
            return true

        val block = id - 0x100
        if (blocking[block] == INVISIBLE) {
            if (isZero[block])
                return true
            val chunkData = (sections[index - 1] as Chunk_v1_18).chunkData
            val last = chunkData.storage
            if (last != null) {
                last.set(block and 0xfff, 0)
                invisible[block] = true
            }
            // If the block in last section is not invisible it should be already false so we don't do a check here.
            return true
        }
        return false
    }

    private inline fun handleBlocking(blocking: ByteArray, isZero: BooleanArray, invisible: BitSet,
                                      x: Int, z: Int, id: Int, storage: BaseStorage): Boolean {
        addNearby(blocking, id, x, z)

        val block = id - 0x100
        if (blocking[block] == INVISIBLE) {
            if (isZero[block])
                return true
            storage.set(block and 0xfff, 0)
            invisible[block] = true
            return true
        }
        return false
    }

    private inline fun addNearby(blocking: ByteArray, id: Int, x: Int, z: Int) {
        if (x > 0 ) (id - 0x001).let { blocking[it] = blocking[it] or X_PLUS  }
        if (x < 15) (id + 0x001).let { blocking[it] = blocking[it] or X_MINUS }
        if (z > 0 ) (id - 0x010).let { blocking[it] = blocking[it] or Z_PLUS  }
        if (z < 15) (id + 0x010).let { blocking[it] = blocking[it] or Z_MINUS }
        (id + 0x100).let { blocking[it] = blocking[it] or Y_MINUS }
    }

    private inline fun checkSectionAllInvisible(allInvisible: Boolean, invisible: BitSet, randomList: IntArray,
                                                sections: Array<BaseChunk>, index: Int) {
        if (allInvisible) {
            val section = sections[index - 1] as Chunk_v1_18
            // Re-fill invisible; This is to fix data with isZero skipped and SingletonPalette ...
            invisible.set((index - 1) shl 12, index shl 12, true)
            section.chunkData.palette = CustomSingletonPalette(randomList.random())
        }
    }

    private fun checkBlockUpdate(player: Player, blockLocation: Vector3i, minHeight: Int = player.world.minHeight) {
        return checkBlockUpdate(player, blockLocation.x, blockLocation.y, blockLocation.z, minHeight)
    }

    private fun checkBlockUpdate(player: Player, x: Int, y: Int, z: Int, minHeight: Int = player.world.minHeight) {
        val fullUpdateThreshold = config.chunkDataThrottle.thresholdToResentWholeChunk
        val updateDistance = config.chunkDataThrottle.updateDistance

        val miniChunks = player.miniChunks
        val groups = buildList {
            for (i in -updateDistance..updateDistance)
                for (j in -updateDistance + abs(i) .. updateDistance - abs(i))
                    for (k in -updateDistance + abs(i) + abs(j) .. updateDistance - abs(i) - abs(j))
                        add(BlockPos(x + i, y + j, z + k))
        }.groupBy {
            Chunk.getChunkKey(it.x shr 4, it.z shr 4)
        }

        val nms = player.nms
        val level = nms.serverLevel()

        for ((chunkKey, blocks) in groups) {
            checkChunkBlockUpdate(player, nms, level, fullUpdateThreshold, miniChunks, chunkKey, blocks, minHeight)
        }
    }

    private fun checkChunkBlockUpdate(player: Player, nms: ServerPlayer, level: ServerLevel, fullUpdateThreshold: Int,
                                      miniChunks: Long2ObjectMap<PlayerChunk>,
                                      chunkKey: Long, blocks: List<BlockPos>, minHeight: Int) {
        val playerChunk = miniChunks[chunkKey] ?: return
        if (playerChunk === FULL_CHUNK) return

        val invisible = playerChunk.invisible
        val updates = blocks.filter {
            val bid = blockKeyChunk(it.x, it.y, it.z, minHeight)
            invisible.safeGet(bid).also { if (it) invisible[bid] = false }
        }
        if (updates.isEmpty()) return

        val (chunkX, chunkZ) = chunkKey.chunkPos
        val chunk = level.getChunkIfLoaded(chunkX, chunkZ) ?: return
        if (fullUpdateThreshold >= 0 && playerChunk.updatedBlocks >= fullUpdateThreshold) {
            try {
                miniChunks[chunkKey] = FULL_CHUNK
                PlayerChunkSender.sendChunk(nms.connection, level, chunk)
                counter.resentChunks++
            } catch (e: Exception) {
                miniChunks[chunkKey] = playerChunk
                throw e
            }
            return
        }
        playerChunk.updatedBlocks += updates.size
        counter.resentBlocks += updates.size

        val groupBy = updates.groupBy {
            SectionPos.of(it)
        }

        for ((section, blocks) in groupBy) {
            val wrapper = if (blocks.size > 1)
                WrapperPlayServerMultiBlockChange(
                    Vector3i(section.x, section.y, section.z), true, blocks.map {
                        WrapperPlayServerMultiBlockChange.EncodedBlock(
                            chunk.getBlockState(it).id,
                            it.x and 0xf, it.y and 0xf, it.z and 0xf
                        )
                    }.toTypedArray()
                )
            else
                blocks.first().let {
                    WrapperPlayServerBlockChange(
                        Vector3i(it.x, it.y, it.z), chunk.getBlockState(it).id
                    )
                }
            PacketEvents.getAPI().playerManager.sendPacketSilently(player, wrapper)
        }
    }

    private fun BitSet.safeGet(index: Int) = if (index in 0 until size()) get(index) else false

    private inline val Player.nms
        get() = (player as CraftPlayer).handle

    private val BlockState.id
        get() = Block.BLOCK_STATE_REGISTRY.getId(this)

    private data class ChunkPos(val x: Int, val z: Int)
    private data class ChunkBlockPos(val x: Int, val y: Int, val z: Int)

    private inline val Long.chunkPos
        get() = ChunkPos((this and 0xffffffffL).toInt(), (this shr 32).toInt())

    private inline fun blockKeyChunk(x: Int, y: Int, z: Int, minHeight: Int): Int {
        return (x and 0xf) or (z and 0xf shl 4) or (y - minHeight shl 8)
    }
    private inline val Int.chunkBlockPos: ChunkBlockPos
        get() = ChunkBlockPos(this and 0xf, this shr 8, this shr 4 and 0xf)

    private inline val Int.blocking
        get() = blockingCache[this]

    private inline val BlockState.blocking: Boolean
        get() = Block.BLOCK_STATE_REGISTRY.getId(this).blocking

    data class PlayerChunk(
        val invisible: BitSet,
        var updatedBlocks: Int = 0,
    )

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
        var resentBlocks: Long = 0,
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