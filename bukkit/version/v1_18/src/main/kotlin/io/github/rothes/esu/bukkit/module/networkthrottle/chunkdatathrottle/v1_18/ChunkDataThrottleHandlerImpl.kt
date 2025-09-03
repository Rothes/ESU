package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_18

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.stream.NetStreamInput
import com.github.retrooper.packetevents.protocol.world.MaterialType
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
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler.Companion.INVISIBLE
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler.Companion.X_MINUS
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler.Companion.X_PLUS
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler.Companion.Y_MINUS
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler.Companion.Z_MINUS
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler.Companion.Z_PLUS
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkSender
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.LevelHandler
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.PalettedContainerReader
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_18.ChunkDataThrottleHandlerImpl.SectionGetter.Companion.container
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.chunkSent
import io.github.rothes.esu.core.util.UnsafeUtils.usObjGetter
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.jpountz.lz4.LZ4Factory
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World.Environment
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.experimental.or
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.nanoseconds
import com.github.retrooper.packetevents.protocol.world.chunk.storage.BitStorage as PEBitStorage

class ChunkDataThrottleHandlerImpl: ChunkDataThrottleHandler,
    PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    companion object {

        const val SECTION_BLOCKS = 16 * 16 * 16
        const val BITS_ALL_TRUE = -1L

        private var LAVA_MIN = Int.MAX_VALUE
        private var LAVA_MAX = Int.MIN_VALUE
        private var BLOCKS_VIEW = BooleanArray(Block.BLOCK_STATE_REGISTRY.size())
        val ITSELF = IntArray(BLOCKS_VIEW.size) { it }
        val FULL_CHUNK = PlayerChunk(BitSet(0))

    }

    private val containerReader by Versioned(PalettedContainerReader::class.java)
    private val levelHandler by Versioned(LevelHandler::class.java)
    private val chunkSender by Versioned(ChunkSender::class.java)

    private val hotDataFile = NetworkThrottleModule.moduleFolder.resolve("minimalChunksData.tmp")
    private val minimalChunks = hashMapOf<Player, Long2ObjectMap<PlayerChunk>>()

    override val counter = ChunkDataThrottleHandler.Counter()

    override fun reload() {
        val nonInvisible = config.chunkDataThrottle.nonInvisibleBlocksOverrides
        BLOCKS_VIEW = PacketEvents.getAPI().serverManager.version.toClientVersion().let { version ->
            BooleanArray(Block.BLOCK_STATE_REGISTRY.size()) { id ->
                val wrapped = WrappedBlockState.getByGlobalId(version, id, false)
                if (wrapped.type.materialType == MaterialType.LAVA) {
                    LAVA_MIN = min(id, LAVA_MIN)
                    LAVA_MAX = max(id, LAVA_MAX)
                }
                val material = try {
                    SpigotConversionUtil.toBukkitBlockData(wrapped).material
                } catch (_: Exception) {
                    SpigotConversionUtil.toBukkitMaterialData(wrapped).itemType
                }
                if (nonInvisible.contains(material))
                    false
                else when (material) {
                    Material.GLOWSTONE -> true
                    Material.BARRIER   -> false
                    else               -> material.isOccluding
                }
            }
        }
    }

    override fun enable() {
        reload()
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
                        if (!player.chunkSent(chunkKey)) {
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

    override fun disable() {
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
        get() = minimalChunks.getOrPut(this) { Long2ObjectOpenHashMap() }

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.isCancelled) return
        when (event.packetType) {
            PacketType.Play.Client.PLAYER_DIGGING -> {
                val wrapper = WrapperPlayClientPlayerDigging(event)
                if (wrapper.action == DiggingAction.START_DIGGING) {
                    val player = event.getPlayer<Player>()
                    val pos = wrapper.blockPosition
                    if (config.chunkDataThrottle.updateOnLegalInteractOnly) {
                        val eye = player.eyeLocation
                        val dist = (eye.x - pos.x).pow(2) + (eye.y - pos.y).pow(2) + (eye.z - pos.z).pow(2)
                        if (dist > 6.0 * 6.0) {
                            return
                        }
                    }
                    checkBlockUpdate(player, wrapper.blockPosition)
                }
            }
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        if (event.isCancelled) return
        try {
            when (event.packetType) {
                PacketType.Play.Server.UNLOAD_CHUNK       -> {
                    val wrapper = WrapperPlayServerUnloadChunk(event)
                    event.getPlayer<Player>().miniChunks.remove(getChunkKey(wrapper.chunkX, wrapper.chunkZ))
                }

                PacketType.Play.Server.MULTI_BLOCK_CHANGE -> {
                    val wrapper = WrapperPlayServerMultiBlockChange(event)
                    val player = event.getPlayer<Player>()
                    val world = player.world
                    val minHeight = world.minHeight
                    for (block in wrapper.blocks) {
                        if (block.blockId.blocksView) {
                            // Only check full chunk if blocks get broken or transformed to non-blocking
                            continue
                        }
                        checkBlockUpdate(player, block.x, block.y, block.z, minHeight)
                    }
                }

                PacketType.Play.Server.BLOCK_CHANGE       -> {
                    val wrapper = WrapperPlayServerBlockChange(event)
                    if (wrapper.blockId.blocksView) {
                        // Only check full chunk if blocks get broken or transformed to non-blocking
                        return
                    }
                    checkBlockUpdate(event.getPlayer(), wrapper.blockPosition)
                }

                PacketType.Play.Server.CHUNK_DATA         -> {
                    val config = config.chunkDataThrottle
                    if (!config.enabled) {
                        return
                    }
//                val tm = System.nanoTime()
                    val wrapper = WrapperPlayServerChunkData(event)
                    val player = event.getPlayer<Player>()
                    val column = wrapper.column
                    val chunkKey = getChunkKey(column.x, column.z)

                    val miniChunks = player.miniChunks
                    if (miniChunks[chunkKey] === FULL_CHUNK) {
                        // This should be a full chunk
                        miniChunks.remove(chunkKey)
                        return
                    }

                    val nms = player.nms
                    val level = levelHandler.level(nms)
                    val minimalHeightInvisibleCheck = config.minimalHeightInvisibleCheck
                    val world = level.bukkit
                    val randomBlockIds = config.antiXrayRandomBlockIds.getOrDefault(world.name)!!

                    val sections = column.chunks
                    val height = event.user.totalWorldHeight
                    val bvArr = ByteArray((height shl 8) + 16 * 16) // BlocksViewArray
                    val invisible = BooleanArray(height shl 8)
                    if (!minimalHeightInvisibleCheck) for (i in 0 until 16 * 16) bvArr[i] = Y_MINUS
                    // Handle neighbour chunks starts
                    handleNeighbourChunk(bvArr, level, column.x + 1, column.z, 0x00, 0x10, +0x0f, X_PLUS)
                    handleNeighbourChunk(bvArr, level, column.x - 1, column.z, 0x0f, 0x10, -0x0f, X_MINUS)
                    handleNeighbourChunk(bvArr, level, column.x, column.z + 1, 0x00, 0x01, +0xf0, Z_PLUS)
                    handleNeighbourChunk(bvArr, level, column.x, column.z - 1, 0xf0, 0x01, -0xf0, Z_MINUS)
                    // Handle neighbour chunks ends

                    class SectionData(
                        val bits: Int,
                        val data: IntArray,
                        val states: IntArray,
                    )

                    val sectionDataArr = Array<SectionData?>(sections.size) { null }
                    var id = 0
                    out@ for ((index, section) in sections.withIndex()) {
                        section as Chunk_v1_18
                        val palette = section.chunkData.palette
                        if (palette is SingletonPalette) {
                            // This section contains only one block type, and it's already the smallest way.
                            if (palette.idToState(0).blocksView) {
                                if (index > 0) {
                                    // Check if the surface on previous section is invisible
                                    checkSurfaceInvisible(bvArr, invisible, id)
                                }
                                id += SECTION_BLOCKS
                                for (i in id until id + 16 * 16) bvArr[i] = bvArr[i] or Y_MINUS
                            } else {
                                id += SECTION_BLOCKS
                            }
                        } else {
                            val data = readBitsData(section.chunkData.storage)
                            val bits = palette.bits
                            val states: IntArray
                            val blockingArr: BooleanArray
                            when (palette) {
                                is ListPalette, is MapPalette -> {
                                    states = IntArray(section.chunkData.palette.size()) { i ->
                                        section.chunkData.palette.idToState(i)
                                    }
                                    blockingArr = BooleanArray(states.size) { i -> states[i].blocksView }
                                }
                                is GlobalPalette -> {
                                    states = ITSELF
                                    blockingArr = BLOCKS_VIEW
                                }
                                else         -> {
                                    error("Unsupported packetevents palette type: ${palette::class.simpleName}")
                                }
                            }

                            for (i in 0 until SECTION_BLOCKS) {
                                if (blockingArr[data[i]]) {
                                    addNearby(bvArr, id)
                                    // Make sure it's not out of bounds, while we are processing bedrock layer
                                    if (id >= 0x100) {
                                        // Check if previous block is complete invisible
                                        val previous = id - 0x100
                                        if (bvArr[previous] == INVISIBLE) {
                                            invisible[previous] = true
                                        }
                                    }
                                }
                                id++
                            }
                            sectionDataArr[index] = SectionData(bits, data, states)
                        }
                    }
                    if (!config.netherRoofInvisibleCheck && world.environment == Environment.NETHER) {
                        // We could do the same thing to the top section,
                        // but it never happens in vanilla generated chunks,
                        // so, no.
                        checkSurfaceInvisible(bvArr, invisible, 0x1000 * (8 - 1))
                    }

                    val array = invisible.toLongArray()
                    id = 0
                    for ((index, section) in sections.withIndex()) {
                        section as Chunk_v1_18
                        val sectionData = sectionDataArr[index]
                        var i = (index + 1 shl 6) - 1
                        if (sectionData == null) {
                            // This section is SingletonPalette
                            if (i < array.size && array[i] == BITS_ALL_TRUE && array[i - 64 + 1] == BITS_ALL_TRUE) {
                                section.chunkData.palette = SingletonPalette(randomBlockIds.random())
                            }
                            id += SECTION_BLOCKS
                            continue
                        }
                        if (i < array.size) {
                            var allInvisible = true
                            for (k in 0 until (SECTION_BLOCKS / 64)) {
                                if (array[i--] != BITS_ALL_TRUE) {
                                    allInvisible = false
                                    break
                                }
                            }
                            if (allInvisible) {
                                section.chunkData.palette = SingletonPalette(randomBlockIds.random())
                                id += SECTION_BLOCKS
                                continue
                            }
                        }
                        // It's not a fully invisible section. Do what we can do to help with compression.
                        val bits: Int
                        val remappedState: IntArray
                        if (sectionData.bits < GlobalPalette.BITS_PER_ENTRY) {
                            // Rebuild palette mapping
                            val frequency = ShortArray(sectionData.states.size)
                            for (i in 0 until SECTION_BLOCKS) {
                                if (!invisible[id++])
                                    frequency[sectionData.data[i]]++
                            }

                            val empty = frequency.count { it == 0.toShort() } // The amount we don't need to put into the new mapping
                            if (frequency.size - empty == 1) {
                                // Only contains 1 block type, we can convert it into SingletonPalette
                                section.chunkData.palette = SingletonPalette(sectionData.states[frequency.indexOfFirst { it != 0.toShort() }])
                                continue
                            }

                            id -= SECTION_BLOCKS // Rollback for the loop below

                            val freqId = IntArray(frequency.size) { it }.sortedByDescending { frequency[it] }.toIntArray()
                            remappedState = IntArray(frequency.size) // value is the new index of original
                            for (i in 0 until remappedState.size) {
                                val oldStateIndex = freqId[i]
                                remappedState[oldStateIndex] = i
                            }

                            bits = (32 - (frequency.size - empty - 1).countLeadingZeroBits())
                                .coerceAtLeast(4) // Vanilla forces at least 4
                            val remapped =
                                if (config.enhancedAntiXray && (
                                       // Check if we can add a block type without adding bits used.
                                       frequency.size - empty - 1 and (1 shl bits - 1) ==
                                       frequency.size - empty     and (1 shl bits - 1) ||
                                       frequency.size - empty + 1 <= (1 shl 4))
                                    ) {
                                    for ((i, v) in remappedState.withIndex()) {
                                        remappedState[i] = v + 1
                                    }
                                    IntArray(frequency.size - empty + 1) { i ->
                                        if (i == 0) randomBlockIds.random()
                                        else sectionData.states[remappedState.indexOf(i)]
                                    }
                                } else {
                                    IntArray(frequency.size - empty) { i -> sectionData.states[remappedState.indexOf(i)] }
                                }

                            section.chunkData.palette = CustomListPalette(bits, remapped)
                        } else {
                            bits = sectionData.bits
                            remappedState = sectionData.states
                        }
                        val valuesPerLong = 64 / bits
                        val longs = (SECTION_BLOCKS + valuesPerLong - 1) / valuesPerLong
                        val new = LongArray(longs)

                        val maxShift = bits * valuesPerLong
                        var cellIndex = 0
                        var shift = 0
                        var l = 0L
                        for (i in 0 until SECTION_BLOCKS) {
                            if (!invisible[id++]) {
                                l = l or (remappedState[sectionData.data[i]].toLong() shl shift)
                            }

                            shift += bits
                            if (shift == maxShift) {
                                shift = 0
                                new[cellIndex++] = l
                                l = 0
                            }
                        }
                        if (l != 0L) {
                            new[cellIndex] = l
                        }

                        section.chunkData.storage = PEBitStorage(bits, SECTION_BLOCKS, new)
                    }

                    counter.minimalChunks++
                    miniChunks[chunkKey] = PlayerChunk(BitSet.valueOf(array))
//                    // benchmark starts
//                    val tim = System.nanoTime() - tm
//                    if (tim < 600_000) timesl.add(tim)
//                    if (timesl.size > 1000) println("AVG: ${timesl.takeLast(1000).average()}")
//                    event.isCancelled = true
                    // benchmark ends
                }
            }
        } catch (t: Throwable) {
            plugin.err("[ChunkDataThrottle] An exception occurred while processing packet", t)
        }
    }
//    private val timesl = LongArrayList(10000000)

    private fun handleNeighbourChunk(blocking: ByteArray, level: ServerLevel, chunkX: Int, chunkZ: Int,
                                            bid: Int, bidStep: Int, arrOffset: Int, arrValue: Byte) {
        val chunk = level.getChunkIfLoaded(chunkX, chunkZ) ?: return

        val indexLoop = 0x100 - bidStep * 16
        var blockId = bid
        for (section in chunk.sections) {
            val states = section.container
            val palette = containerReader.getPalette(states)
            if (palette is SingleValuePalette<BlockState>) {
                if (palette.valueFor(0).blocksView) {
                    for (y in 0 until 16) {
                        for (j in 0 until 16) {
                            (blockId + arrOffset).let { blocking[it] = blocking[it] or arrValue }
                            blockId += bidStep
                        }
                        blockId += indexLoop
                    }
                } else {
                    blockId += SECTION_BLOCKS
                }
            } else {
                val storage = containerReader.getStorage(states)
                val blockingArr = when (palette) {
                    is LinearPalette<BlockState>, is HashMapPalette<BlockState> ->
                        BooleanArray(palette.size) { id -> palette.valueFor(id).blocksView }
                    is net.minecraft.world.level.chunk.GlobalPalette<BlockState> ->
                        BLOCKS_VIEW
                    else ->
                        error("Unsupported minecraft palette type: ${palette::class.simpleName}")
                }

                for (y in 0 until 16) {
                    for (j in 0 until 16) {
                        if (blockingArr[storage.get(blockId and 0xfff)])
                            (blockId + arrOffset).let { blocking[it] = blocking[it] or arrValue }
                        blockId += bidStep
                    }
                    blockId += indexLoop
                }
            }
        }
    }

    private fun checkSurfaceInvisible(bvArr: ByteArray, invisible: BooleanArray, id: Int) {
        for (i in id - 1 downTo id - 0x101) {
            if (bvArr[i] == INVISIBLE) {
                invisible[i] = true
                return
            }
        }
    }

    private fun addNearby(blocking: ByteArray, id: Int) {
        val x = id and 0xf
        val z = id shr 4 and 0xf
        if (x != 0 ) (id - 0x001).let { blocking[it] = blocking[it] or X_PLUS  }
        if (x != 15) (id + 0x001).let { blocking[it] = blocking[it] or X_MINUS }
        if (z != 0 ) (id - 0x010).let { blocking[it] = blocking[it] or Z_PLUS  }
        if (z != 15) (id + 0x010).let { blocking[it] = blocking[it] or Z_MINUS }
        (id + 0x100).let { blocking[it] = blocking[it] or Y_MINUS }
    }

    private fun BooleanArray.toLongArray(): LongArray {
        val count = size shr 6
        val arr = LongArray(count)

        var i = 0
        for (j in 0 until count) {
            var l = 0L
            for (k in 0 until 64) {
                if (this[i++])
                    l = l or (0b1L shl k)
            }
            arr[j] = l
        }
        return arr
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
            getChunkKey(it.x shr 4, it.z shr 4)
        }

        val nms = player.nms
        val level = levelHandler.level(nms)

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
                chunkSender.sendChunk(nms, level, chunk)
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

    private val Level.bukkit
        get() = this.world

    private val Player.nms: ServerPlayer
        get() = (this as CraftPlayer).handle

    private val BlockState.id
        get() = Block.BLOCK_STATE_REGISTRY.getId(this)

    private data class ChunkPos(val x: Int, val z: Int)
    private data class ChunkBlockPos(val x: Int, val y: Int, val z: Int)

    private val Long.chunkPos
        get() = ChunkPos((this and 0xffffffffL).toInt(), (this shr 32).toInt())

    private fun blockKeyChunk(x: Int, y: Int, z: Int, minHeight: Int): Int {
        return (x and 0xf) or (z and 0xf shl 4) or (y - minHeight shl 8)
    }
    private fun getChunkKey(x: Int, z: Int): Long {
        return x.toLong() and 0xffffffffL or ((z.toLong() and 0xffffffffL) shl 32)
    }
    private val Int.chunkBlockPos: ChunkBlockPos
        get() = ChunkBlockPos(this and 0xf, this shr 8, this shr 4 and 0xf)

    private val Int.blocksView
        get() = BLOCKS_VIEW[this]

    private val BlockState.blocksView: Boolean
        get() = Block.BLOCK_STATE_REGISTRY.getId(this).blocksView

    data class PlayerChunk(
        val invisible: BitSet,
        var updatedBlocks: Int = 0,
    )

    fun readBitsData(bitStorage: BaseStorage) = readBitsData(bitStorage.data, bitStorage.bitsPerEntry)

    fun readBitsData(data: LongArray, bits: Int): IntArray {
        val mask = (1L shl bits) - 1L
        val valuesPerLong = 64 / bits

        var cellIndex = 0
        var l = 0L
        var read = valuesPerLong - 1

        val array = IntArray(SECTION_BLOCKS)

        for (i in 0 until SECTION_BLOCKS) {
            if (++read == valuesPerLong) {
                l = data[cellIndex++]
                read = 0
            }
            array[i] = (l and mask).toInt()
            l = l shr bits
        }
        return array
    }

    private class CustomListPalette(bits: Int, array: IntArray): ListPalette(bits, CustomNetStreamInput(array)) {

        private class CustomNetStreamInput(val array: IntArray) : NetStreamInput(null) {
            private var read = -2
            override fun readVarInt(): Int {
                return if (++read == -1) array.size else array[read]
            }
        }
    }

    interface SectionGetter {

        fun getContainer(section: LevelChunkSection): PalettedContainer<BlockState>

        companion object {

            val instance = if (ServerCompatibility.isPaper) Paper else CB

            val LevelChunkSection.container: PalettedContainer<BlockState>
                get() = instance.getContainer(this)
        }

        private object CB: SectionGetter {

            // This field is private on Spigot
            private val getter = LevelChunkSection::class.java.declaredFields.first { it.type == PalettedContainer::class.java }.usObjGetter

            override fun getContainer(section: LevelChunkSection): PalettedContainer<BlockState> {
                @Suppress("UNCHECKED_CAST")
                return getter[section] as PalettedContainer<BlockState>
            }

        }

        private object Paper: SectionGetter {

            override fun getContainer(section: LevelChunkSection): PalettedContainer<BlockState> {
                return section.states
            }

        }
    }

}