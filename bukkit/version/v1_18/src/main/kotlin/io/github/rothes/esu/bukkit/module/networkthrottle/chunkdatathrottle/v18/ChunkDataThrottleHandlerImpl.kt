package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v18

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.stream.NetStreamInput
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18
import com.github.retrooper.packetevents.protocol.world.chunk.palette.GlobalPalette
import com.github.retrooper.packetevents.protocol.world.chunk.palette.ListPalette
import com.github.retrooper.packetevents.protocol.world.chunk.palette.MapPalette
import com.github.retrooper.packetevents.protocol.world.chunk.palette.SingletonPalette
import com.github.retrooper.packetevents.protocol.world.chunk.storage.BaseStorage
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk
import com.google.common.io.ByteStreams
import com.google.common.primitives.Ints
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v18.ChunkDataThrottleHandlerImpl.SectionGetter.Companion.container
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.CoordinateUtils
import io.github.rothes.esu.bukkit.util.CoordinateUtils.chunkPos
import io.github.rothes.esu.bukkit.util.CoordinateUtils.getChunkKey
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.chunkSent
import io.github.rothes.esu.bukkit.util.version.adapter.nms.BlockOccludeTester
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ChunkSender
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.PalettedContainerReader
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.serializer.MapSerializer.DefaultedLinkedHashMap
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import io.github.rothes.esu.core.util.extension.forEachInt
import io.github.rothes.esu.lib.configurate.objectmapping.meta.PostProcess
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.jpountz.lz4.LZ4Factory
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World.Environment
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.nanoseconds
import com.github.retrooper.packetevents.protocol.world.chunk.storage.BitStorage as PEBitStorage

object ChunkDataThrottleHandlerImpl: ChunkDataThrottleHandler<ChunkDataThrottleHandlerImpl.HandlerConfig, Unit>(), Listener {

    // The block B is in center. if Y_MINUS, block in B's bottom is occluding(i.e. blocking) .
    const val X_PLUS: Byte    = 0b00_00001
    const val X_MINUS: Byte   = 0b00_00010
    const val Z_PLUS: Byte    = 0b00_00100
    const val Z_MINUS: Byte   = 0b00_01000
    const val Y_MINUS: Byte   = 0b00_10000
    const val INVISIBLE: Byte = 0b00_11111

    const val X_LAVA: Byte    = 0b01_00000
    const val Z_LAVA: Byte    = 0b10_00000

    const val SECTION_BLOCKS = 16 * 16 * 16

    const val BV_VISIBLE: Byte = 0b0
    const val BV_INVISIBLE: Byte = 0b1
    const val BV_LAVA_COVERED: Byte = 0b1011 // Why this value? flowing-lava id is 11, also the bit on BV_UPPER_OCCLUDING is 1

    const val BV_UPPER_OCCLUDING: Byte = 0b10 // Visible, but the block on its top is occluding

    private var LAVA_MIN = Int.MAX_VALUE
    private var LAVA_MAX = Int.MIN_VALUE
    private var BLOCKS_VIEW = ByteArray(Block.BLOCK_STATE_REGISTRY.size())
    private val ITSELF = IntArray(BLOCKS_VIEW.size) { it }
    private val FULL_CHUNK = PlayerChunk(BitSet(0))

    private fun Boolean.toByte(): Byte = if (this) 1 else 0

    private val containerReader by Versioned(PalettedContainerReader::class.java)
    private val levelHandler by Versioned(LevelHandler::class.java)
    private val chunkSender by Versioned(ChunkSender::class.java)

    private val hotDataFile = NetworkThrottleModule.moduleFolder.resolve("minimalChunksData.tmp")
    private val minimalChunks = hashMapOf<Player, Long2ObjectMap<PlayerChunk>>()

    private var previousNonInvisible: Set<Block>? = null
    private var wasEnabled = false

    override fun onReload() {
        super.onReload()
        if (enabled) buildCache()
    }

    override fun onEnable() {
        wasEnabled = true
        buildCache()
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
        PacketEvents.getAPI().eventManager.registerListener(PacketListener)
        register()
    }

    override fun onTerminate() {
        super.onTerminate()
        if (!wasEnabled) return
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListener)
        unregister()

        if (plugin.disabledHot) {
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
            hotDataFile.toFile().deleteOnExit()
            plugin.info("Saved ChunkDataThrottle hotData in ${(System.nanoTime() - nanoTime).nanoseconds}. Size ${hotDataFile.fileSize()}")
        }
        // Clear these so we can save our memory
        minimalChunks.values.forEach { it.clear() }
        minimalChunks.clear()
    }

    private fun buildCache() {
        if (previousNonInvisible != config.nonInvisibleBlocksOverrides) {
            val occludeTester = BlockOccludeTester::class.java.versioned()
            val nonInvisible = config.nonInvisibleBlocksOverrides
            BLOCKS_VIEW = ByteArray(Block.BLOCK_STATE_REGISTRY.size()) { id ->
                val blockState = Block.BLOCK_STATE_REGISTRY.byId(id)!!
                val block = blockState.block
                if (block == Blocks.LAVA) {
                    LAVA_MIN = min(id, LAVA_MIN)
                    LAVA_MAX = max(id, LAVA_MAX)
                }
                if (nonInvisible.contains(block)) false.toByte()
                else occludeTester.isFullOcclude(blockState).toByte()
            }.apply {
                for (i in LAVA_MIN..LAVA_MAX) {
                    this[i] = BV_LAVA_COVERED
                }
            }
            previousNonInvisible = nonInvisible
        }
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(e: PlayerMoveEvent) {
        if (!config.detectLavaPool)
            return
        val player = e.player.nms
        val level = levelHandler.level(player)
        // Need to use chunk.getBlockState on Folia
        val chunk = level.getChunkIfLoaded(player.blockPosition()) ?: return
        val pos = listOf(player.blockPosition(), player.blockPosition().offset(0, 1, 0), player.blockPosition().offset(0, -1, 0))
        if (pos.any { chunk.getBlockState(it).bukkitMaterial == Material.LAVA }) {
            checkBlockUpdate(player.bukkitEntity, player.blockPosition())
        }
    }

    private fun handleNeighbourChunk(blocking: ByteArray, level: ServerLevel, chunkX: Int, chunkZ: Int,
                                            bid: Int, bidStep: Int, arrOffset: Int, arrValue: Byte, lavaValue: Byte) {
        val chunk = level.getChunkIfLoaded(chunkX, chunkZ) ?: return

        val indexLoop = 0x100 - bidStep * 16
        var blockId = bid
        for (section in chunk.sections) {
            val states = section.container
            val palette = containerReader.getPalette(states)
            if (palette is SingleValuePalette<BlockState>) {
                if (palette.valueFor(0).blocksView != BV_VISIBLE) {
                    val or = when (palette.valueFor(0).blocksView) {
                        BV_INVISIBLE    -> arrValue
                        BV_LAVA_COVERED -> lavaValue
                        else            -> throw AssertionError()
                    }
                    for (y in 0 until 16) {
                        for (j in 0 until 16) {
                            (blockId + arrOffset).let { blocking[it] = blocking[it] or or }
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
                        ByteArray(palette.size + 1).apply {
                            // We add one size for safe, cuz this is not thread-safe,
                            // Players might place extra blocks to the chunk during the process below (storage.get())
                            for (i in 0 until palette.size)
                                this[i] = palette.valueFor(i).blocksView
                        }
                    is net.minecraft.world.level.chunk.GlobalPalette<BlockState> ->
                        BLOCKS_VIEW
                    else ->
                        error("Unsupported minecraft palette type: ${palette::class.simpleName}")
                }

                for (y in 0 until 16) {
                    for (j in 0 until 16) {
                        when (blockingArr[storage.get(blockId and 0xfff)]) {
                            BV_INVISIBLE    -> (blockId + arrOffset).let { blocking[it] = blocking[it] or arrValue }
                            BV_LAVA_COVERED -> (blockId + arrOffset).let { blocking[it] = blocking[it] or lavaValue }
                        }
                        blockId += bidStep
                    }
                    blockId += indexLoop
                }
            }
        }
    }

    private fun checkSurfaceInvisible(bvArr: ByteArray, invisible: ByteArray, id: Int, setTo: Byte) {
        for (i in id - 1 downTo id - 0x101) {
            if (bvArr[i] and INVISIBLE == INVISIBLE) {
                invisible[i] = setTo
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

    private fun ByteArray.toLongArray(): LongArray {
        val count = size shr 6
        val arr = LongArray(count)

        var i = 0
        for (j in 0 until count) {
            var l = 0L
            for (k in 0 until Long.SIZE_BITS) {
                if (this[i++] == BV_INVISIBLE)
                    l = l or (0b1L shl k)
            }
            arr[j] = l
        }
        return arr
    }

    private fun checkBlockUpdate(player: Player, blockPos: BlockPos, minHeight: Int = player.world.minHeight) {
        return checkBlockUpdate(player, blockPos.x, blockPos.y, blockPos.z, minHeight)
    }

    private fun checkBlockUpdate(player: Player, blockLocation: Vector3i, minHeight: Int = player.world.minHeight) {
        return checkBlockUpdate(player, blockLocation.x, blockLocation.y, blockLocation.z, minHeight)
    }

    private fun checkBlockUpdate(player: Player, x: Int, y: Int, z: Int, minHeight: Int = player.world.minHeight) {
        val fullUpdateThreshold = config.thresholdToResentWholeChunk
        val updateDistance = config.updateDistance

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
        val updates = blocks.filter { blockPos ->
            val bid = CoordinateUtils.getBlockChunkKey(blockPos.x, blockPos.y - minHeight, blockPos.z)
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
            val wrapper = if (blocks.size > 1) WrapperPlayServerMultiBlockChange(
                Vector3i(section.x, section.y, section.z), true, blocks.map {
                    WrapperPlayServerMultiBlockChange.EncodedBlock(
                        chunk.getBlockState(it).id, it.x and 0xf, it.y and 0xf, it.z and 0xf
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

    private val Int.blocksView
        get() = BLOCKS_VIEW[this]

    private val BlockState.blocksView
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

    private object PacketListener: PacketListenerAbstract(PacketListenerPriority.HIGHEST) {

        override fun onPacketReceive(event: PacketReceiveEvent) {
            if (event.isCancelled) return
            when (event.packetType) {
                PacketType.Play.Client.PLAYER_DIGGING -> {
                    val wrapper = WrapperPlayClientPlayerDigging(event)
                    if (wrapper.action == DiggingAction.START_DIGGING) {
                        val player = event.getPlayer<Player>()
                        val pos = wrapper.blockPosition
                        if (config.updateOnLegalInteractOnly) {
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
                            if (block.blockId.blocksView == BV_INVISIBLE) {
                                // Only check update if blocks get broken or transformed to non-blocksView
                                continue
                            }
                            checkBlockUpdate(player, block.x, block.y, block.z, minHeight)
                        }
                    }

                    PacketType.Play.Server.BLOCK_CHANGE       -> {
                        val wrapper = WrapperPlayServerBlockChange(event)
                        if (wrapper.blockId.blocksView == BV_INVISIBLE) {
                            // Only check update if blocks get broken or transformed to non-blocksView
                            return
                        }
                        checkBlockUpdate(event.getPlayer(), wrapper.blockPosition)
                    }

                    PacketType.Play.Server.CHUNK_DATA         -> {
                        val config = config
                        if (!enabled) {
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
                        val invisible = ByteArray(height shl 8) // Stores BV_ bytes
                        if (!minimalHeightInvisibleCheck) for (i in 0 until 16 * 16) bvArr[i] = Y_MINUS
                        // Handle neighbour chunks starts
                        handleNeighbourChunk(bvArr, level, column.x + 1, column.z, 0x00, 0x10, +0x0f, X_PLUS,  X_LAVA)
                        handleNeighbourChunk(bvArr, level, column.x - 1, column.z, 0x0f, 0x10, -0x0f, X_MINUS, X_LAVA)
                        handleNeighbourChunk(bvArr, level, column.x, column.z + 1, 0x00, 0x01, +0xf0, Z_PLUS,  Z_LAVA)
                        handleNeighbourChunk(bvArr, level, column.x, column.z - 1, 0xf0, 0x01, -0xf0, Z_MINUS, Z_LAVA)
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
                                if (palette.idToState(0).blocksView != BV_VISIBLE) {
                                    if (index > 0) {
                                        // Check if the surface on previous section is invisible
                                        checkSurfaceInvisible(bvArr, invisible, id, palette.idToState(0).blocksView)
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
                                val blockingArr: ByteArray
                                when (palette) {
                                    is ListPalette, is MapPalette -> {
                                        states = IntArray(section.chunkData.palette.size()) { i ->
                                            section.chunkData.palette.idToState(i)
                                        }
                                        blockingArr = ByteArray(states.size) { i -> states[i].blocksView }
                                    }
                                    is GlobalPalette              -> {
                                        states = ITSELF
                                        blockingArr = BLOCKS_VIEW
                                    }
                                    else         -> {
                                        error("Unsupported packetevents palette type: ${palette::class.simpleName}")
                                    }
                                }

                                for (i in 0 until SECTION_BLOCKS) {
                                    when (blockingArr[data[i]]) {
                                        BV_INVISIBLE    -> {
                                            addNearby(bvArr, id)
                                            // Make sure it's not out of bounds, while we are processing bedrock layer
                                            if (id >= 0x100) {
                                                // Check if previous block is complete invisible
                                                val previous = id - 0x100
                                                invisible[previous] = if (bvArr[previous] and INVISIBLE == INVISIBLE) BV_INVISIBLE else BV_UPPER_OCCLUDING
                                            }
                                        }
                                        BV_LAVA_COVERED -> {
                                            if (id >= 0x100) {
                                                invisible[id - 0x100] = BV_LAVA_COVERED
                                            }
                                        }
                                    }
                                    id++
                                }
                                sectionDataArr[index] = SectionData(bits, data, states)
                            }
                        }
                        if (config.detectInvisibleSingleBlock) {
                            val pending = IntArrayList()
                            id = invisible.size - 16 * 16 + 1
                            while (--id >= 16 * 16) {
                                if (invisible[id] != BV_INVISIBLE) continue // Center block is invisible
                                val x = id and 0xf
                                val z = id shr 4 and 0xf
                                // Surrounded blocks are visible and upper block occluding
                                if (invisible[id - 0x100] == BV_INVISIBLE) continue // Could be BV_LAVA_COVERED and BV_VISIBLE, due to center block
                                if (invisible[id + 0x100] and BV_UPPER_OCCLUDING != BV_UPPER_OCCLUDING) continue
                                if (x != 0  && invisible[id - 0x001] and BV_UPPER_OCCLUDING != BV_UPPER_OCCLUDING) continue
                                if (x != 15 && invisible[id + 0x001] and BV_UPPER_OCCLUDING != BV_UPPER_OCCLUDING) continue
                                if (z != 0  && invisible[id - 0x010] and BV_UPPER_OCCLUDING != BV_UPPER_OCCLUDING) continue
                                if (z != 15 && invisible[id + 0x010] and BV_UPPER_OCCLUDING != BV_UPPER_OCCLUDING) continue
                                // It's the case.
                                addNearby(bvArr, id)
                                invisible[id - 0x100] = BV_UPPER_OCCLUDING
                                // Use pending, there might be piled single-block
                                pending.add(id - 0x100)
                                pending.add(id + 0x100)
                                if (x != 0 ) pending.add(id - 0x001)
                                if (x != 15) pending.add(id + 0x001)
                                if (z != 0 ) pending.add(id - 0x010)
                                if (z != 15) pending.add(id + 0x010)
                            }
                            pending.forEachInt { i ->
                                if (bvArr[i] and INVISIBLE == INVISIBLE) {
                                    invisible[i] = BV_INVISIBLE
                                }
                            }
                        }
                        if (config.detectLavaPool) {
                            val pending = IntArrayList()
                            id = -1
                            while (++id < invisible.size) {
                                if (invisible[id] != BV_LAVA_COVERED) continue

                                val x = id and 0xf
                                val z = id shr 4 and 0xf
                                fun checkBlock(id: Int, b: Byte) {
                                    if (invisible[id] == BV_LAVA_COVERED) {
                                        bvArr[id] = bvArr[id] or b
                                    }
                                }
                                fun checkEdge(id: Int, check: Byte, set: Byte) {
                                    // it and upper block should both be lava-covered
                                    if (bvArr[id] and check == check && bvArr[id + 0x100] and check == check) {
                                        bvArr[id] = bvArr[id] or set
                                    }
                                }
                                if (x != 0 ) checkBlock(id - 0x001, X_PLUS ) else checkEdge(id, X_LAVA, X_MINUS)
                                if (x != 15) checkBlock(id + 0x001, X_MINUS) else checkEdge(id, X_LAVA, X_PLUS )
                                if (z != 0 ) checkBlock(id - 0x010, Z_PLUS ) else checkEdge(id, Z_LAVA, Z_MINUS)
                                if (z != 15) checkBlock(id + 0x010, Z_MINUS) else checkEdge(id, Z_LAVA, Z_PLUS )
                                checkBlock(id + 0x100, Y_MINUS)
                                pending.add(id)
                            }
                            pending.forEachInt { i ->
                                if (bvArr[i] and INVISIBLE == INVISIBLE) {
                                    invisible[i] = BV_INVISIBLE
                                }
                            }
                        }
                        if (!config.netherRoofInvisibleCheck && world.environment == Environment.NETHER) {
                            // We could do the same thing to the top section,
                            // but it never happens in vanilla generated chunks,
                            // so, no.
                            checkSurfaceInvisible(bvArr, invisible, 0x1000 * (8 - 1), BV_INVISIBLE)
                        }

                        id = 0
                        section@ for ((index, section) in sections.withIndex()) {
                            section as Chunk_v1_18
                            val sectionData = sectionDataArr[index]
                            if (sectionData == null) {
                                // This section is SingletonPalette
                                for (i in id until id + 16 * 16) {
                                    if (invisible[i] != BV_INVISIBLE) {
                                        id += SECTION_BLOCKS
                                        continue@section
                                    }
                                }
                                for (i in id + 16 * 16 * 15 until id + 16 * 16 * 16) {
                                    if (invisible[i] != BV_INVISIBLE) {
                                        id += SECTION_BLOCKS
                                        continue@section
                                    }
                                }
                                val newState = randomBlockIds.random()
                                // detectSameStateUpdate
                                if (section.chunkData.palette.idToState(0) == newState)
                                    invisible.fill(BV_VISIBLE, id, id + SECTION_BLOCKS)
                                section.chunkData.palette = SingletonPalette(newState)
                                id += SECTION_BLOCKS
                                continue
                            }
                            var allInvisible = true
                            for (i in id until id + SECTION_BLOCKS) {
                                if (invisible[i] != BV_INVISIBLE) {
                                    allInvisible = false
                                    break
                                }
                            }
                            if (allInvisible) {
                                val newState = randomBlockIds.random()
                                // detectSameStateUpdate
                                for (i in 0 until SECTION_BLOCKS) {
                                    if (sectionData.states[sectionData.data[i]] == newState) {
                                        invisible[id] = BV_VISIBLE
                                    }
                                    id++
                                }
                                section.chunkData.palette = SingletonPalette(newState)
                                continue
                            }
                            // It's not a fully invisible section. Do what we can do to help with compression.
                            val bits: Int
                            val remappedState: IntArray
                            if (sectionData.bits < GlobalPalette.BITS_PER_ENTRY) {
                                // Rebuild palette mapping
                                val frequency = ShortArray(sectionData.states.size)
                                for (i in 0 until SECTION_BLOCKS) {
                                    if (invisible[id++] != BV_INVISIBLE)
                                        frequency[sectionData.data[i]]++
                                }

                                val empty = frequency.count { it == 0.toShort() } // The amount we don't need to put into the new mapping
                                if (frequency.size - empty == 1) {
                                    // Only contains 1 block type, we can convert it into SingletonPalette
                                    section.chunkData.palette =
                                        SingletonPalette(sectionData.states[frequency.indexOfFirst { it != 0.toShort() }])
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
                                when (invisible[id]) {
                                    BV_INVISIBLE -> {
                                        // detectSameStateUpdate
                                        if (remappedState[0] == sectionData.states[sectionData.data[i]]) {
                                            invisible[id] == BV_VISIBLE
                                        }
                                    }
                                    else -> l = l or (remappedState[sectionData.data[i]].toLong() shl shift)
                                }
                                id++

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
                        miniChunks[chunkKey] = PlayerChunk(BitSet.valueOf(invisible.toLongArray()))
                        // benchmark starts
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
//        private val timesl = LongArrayList(10000000)
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
            private val getter = LevelChunkSection::class.java.declaredFields.first { it.type == PalettedContainer::class.java }.usObjAccessor

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

    data class HandlerConfig(
        @Comment("""
                Plugin will resent complete original chunk data if resent block amount exceeds this value.
                Set it to -1 will never resent chunk but keep updating nearby blocks, 
                 0 to always resent original chunks.
                Set this to a large value can prevent constantly sending block update packets.
                Original chunk is not with anti-xray functionality. It is recommended to leave this value -1 .
                """)
        val thresholdToResentWholeChunk: Int = -1,
        @Comment("""
                We updates the nearby blocks when a player digs a block immediately.
                If this is enabled, we will check if the block is in the interaction range
                 of the player with a rough calculation.
                """)
        val updateOnLegalInteractOnly: Boolean = true,
        @Comment("How many distance of blocks to update from the center when necessary.")
        val updateDistance: Int = 2,
        @Comment("""
                The bedrock level(minimal height) is never visible unless you are in void.
                We would skip the check, and if you don't like it you can enable it.
                """)
        val minimalHeightInvisibleCheck: Boolean = false,
        @Comment("""
                Same with minimal-height but it's for nether roof. For out-of-the-box, it's true by default.
                It's highly recommend to set it to FALSE if you don't allow players to get above there.
                """)
        val netherRoofInvisibleCheck: Boolean = true,
        @Comment("""
                If a non-occluding block is surrounded by occluding blocks, the center block is invisible.
                But should we consider all surrounded blocks invisible to this block face?
                Unless the player joins the game with their eye in the non-occluding block,
                 they will never naturally see those surrounded blocks.
                This step takes extra ~0.02ms, so it's not enabled by default.
                Enable this could help with saving bandwidth in nether, as there's many single-block lava.
            """)
        val detectInvisibleSingleBlock: Boolean = false,
        @Comment("""
                Detect lava pool, and consider lava blocks which being covered invisible.
                This step takes extra ~0.03ms, so it's not enabled by default.
                It also makes the plugin detect nearby blocks everytime player moves.
                Enable this could help with saving bandwidth, especially in nether.
            """)
        val detectLavaPool: Boolean = false,
        @Comment("""
                This feature doesn't support running along with any other anti-xray plugins.
                You must use the anti-xray here we provide.
                
                We will send non-visible blocks to one of the random block in this list.
                If you don't like to anti-xray, you can set the list to 'bedrock'.
            """)
        val antiXrayRandomBlockList: DefaultedLinkedHashMap<String, MutableList<Block>> = DefaultedLinkedHashMap<String, MutableList<Block>>(
            mutableListOf(Blocks.BEDROCK)
        ).apply {
            put("world", buildList {
                val cavesUpdate = ServerCompatibility.serverVersion >= 17
                add(Blocks.COAL_ORE)
                if (cavesUpdate) add(Blocks.COPPER_ORE)
                addAll(listOf(Blocks.IRON_ORE, Blocks.GOLD_ORE,
                    Blocks.EMERALD_ORE, Blocks.DIAMOND_ORE, Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE))

                if (cavesUpdate) addAll(listOf(Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_COPPER_ORE,
                    Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                    Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_LAPIS_ORE))
            }.toMutableList())
            put("world_nether", buildList {
                add(Blocks.NETHER_QUARTZ_ORE)
                if (ServerCompatibility.serverVersion >= 16) {
                    add(Blocks.NETHER_GOLD_ORE)
                    add(Blocks.ANCIENT_DEBRIS)
                }
            }.toMutableList())
            put("world_the_end", mutableListOf(Blocks.END_STONE))
        },
        @Comment("""
                If enabled, we add a extra block type to chunk section palettes for the random block.
                This will greatly enhance anti-xray capabilities while giving only few bytes of additional bandwidth.
            """)
        val enhancedAntiXray: Boolean = true,
        @Comment("""
                Put any blocks you don't want to hide, so they are ignored while processing.
                For example, you can add any ores to it, so there's no anti-xray effect.
                WARNING: This significantly reduces compression badly. Please make sure you really have to do this.
            """)
        val nonInvisibleBlocksOverrides: Set<Block> = setOf(),
    ) {

        val antiXrayRandomBlockIds by lazy {
            with(antiXrayRandomBlockList) {
                DefaultedLinkedHashMap<String, IntArray>((default ?: listOf(Blocks.BEDROCK)).toIdArray())
                    .also { map ->
                        map.putAll(entries.map { it.key to it.value.toIdArray() })
                    }
            }
        }

        private fun List<Block>.toIdArray(): IntArray =
            this.map { Block.BLOCK_STATE_REGISTRY.getId(it.defaultBlockState()) }.toIntArray()

        @PostProcess
        private fun postProcess() {
            fun checkEmptyBlockList(key: String, list: MutableList<Block>) {
                if (list.isEmpty()) {
                    list.add(Blocks.BEDROCK)
                    plugin.warn("[ChunkDataThrottle] Anti-xray random block list of '$key' is empty! We have added bedrock to it.")
                }
            }
            antiXrayRandomBlockList.default?.let {
                checkEmptyBlockList("default", it)
            }
            antiXrayRandomBlockList.entries.toList().forEach {
                checkEmptyBlockList(it.key, it.value)
            }
        }
    }

}