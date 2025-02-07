package io.github.rothes.esu.bukkit.module.networkthrottle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18
import com.github.retrooper.packetevents.protocol.world.chunk.palette.ListPalette
import com.github.retrooper.packetevents.protocol.world.chunk.palette.MapPalette
import com.github.retrooper.packetevents.protocol.world.chunk.palette.SingletonPalette
import com.github.retrooper.packetevents.protocol.world.chunk.storage.BaseStorage
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.data
import io.github.rothes.esu.bukkit.module.networkthrottle.ChunkDataThrottle.WorldCache.ChunkCache
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
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
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

object ChunkDataThrottle: PacketListenerAbstract(PacketListenerPriority.HIGHEST), Listener {

    private val FULL_CHUNK = BooleanArray(0)

    private val worldCache = Object2ObjectOpenHashMap<String, WorldCache>()
    private val minimalChunks = hashMapOf<Player, Long2ObjectOpenHashMap<BooleanArray>>()
    private val occludeCache = OccludeCache()
    val counter = Counter()

    private var task: ScheduledTask? = null

    private fun worldCache(world: World) = worldCache.getOrPut(world.name) { WorldCache(world.name) }

    init {
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
    }

    fun onEnable() {
        PacketEvents.getAPI().eventManager.registerListener(this)
        Bukkit.getPluginManager().registerEvents(this, plugin)
        task = Scheduler.async(0, 30 * 60 * 20) {
            val expire = System.currentTimeMillis() - config.chunkDataThrottle.cacheExpireTicks * 50
            for (worldCache in worldCache.values) {
                val iterator = worldCache.chunks.iterator()
                while (iterator.hasNext()) {
                    val (chunkKey, chunkCache) = iterator.next()
                    if (chunkCache.lastAccess < expire) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun onDisable() {
        PacketEvents.getAPI().eventManager.unregisterListener(this)
        HandlerList.unregisterAll(this)

        if (plugin.isEnabled || plugin.disabledHot) {
            for ((player, map) in minimalChunks.entries) {
                map.forEach { (k, v) ->
                    if (v !== FULL_CHUNK) {
                        data.minimalChunks.computeIfAbsent(player.uniqueId) { arrayListOf() }.add(k)
                    }
                }
            }
        }
        task?.cancel()
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
        if (e.action != Action.LEFT_CLICK_BLOCK || e.hand != EquipmentSlot.HAND) return
        val block = e.clickedBlock ?: return

        checkBlockUpdate(e.player, block.x, block.y, block.z)
    }

    private val Player.miniChunks
        get() = this@ChunkDataThrottle.minimalChunks.getOrPut(this) { Long2ObjectOpenHashMap() }

    override fun onPacketSend(event: PacketSendEvent) {
        if (!config.chunkDataThrottle.enabled) {
            return
        }
        when (event.packetType) {
//            PacketType.Play.Server.EXPLOSION    -> {
//                val wrapper = WrapperPlayServerExplosion(event)
//                when (wrapper.blockInteraction) {
//                    WrapperPlayServerExplosion.BlockInteraction.DESTROY_BLOCKS,
//                    WrapperPlayServerExplosion.BlockInteraction.DECAY_DESTROYED_BLOCKS -> {
//                        val player = event.getPlayer<Player>()
//                        val world = player.world
//                        val minHeight = world.minHeight
//                        for (location in wrapper.records)
//                            checkBlockUpdate(player, location, minHeight)
//                    }
//                    else -> return
//                }
//            }
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
                val player = event.getPlayer<Player>()
                if (wrapper.blockId.occlude) {
                    // Only check full chunk if blocks get broken or transformed to non-occlude
                    return
                }
                checkBlockUpdate(player, wrapper.blockPosition)
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
                val sections = column.chunks

                val minHeight = world.minHeight
                val maxHeight = world.maxHeight
                val height = maxHeight - minHeight
                val maxHeightToProceed = config.chunkDataThrottle.maxHeightToProceed

                val wc = worldCache(world)
                val cc = wc.chunks[chunkKey]
                if (cc != null) {
                    val clone = cc.invisible.clone()
                    var i = 0
                    var id = 0
                    out@ for (chunk in sections) {
                        chunk as Chunk_v1_18
                        val storage = chunk.chunkData.storage
                        if (storage == null) {
                            id += 16 * 16 * 16
                            i += 16
                            continue
                        }
                        for (y in 0 ..< 16) {
                            for (zx in 0 ..< 16 * 16) {
                                if (clone[id]) {
                                    storage.set(id and 0xfff, 0)
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
                val invisible = chunkCache.invisible
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

                val occlude = BooleanArray(16 * 16 * height)
                var i = 0
                var id = 0
                out@ for ((index, section) in sections.withIndex()) {
                    section as Chunk_v1_18
                    val palette = section.chunkData.palette
                    when (palette) {
                        is SingletonPalette -> {
                            // This section contains only one block type, and it's already the smallest way.
                            if (palette.idToState(0).occlude) {
                                forChunk2D { x, z ->
                                    handleOccludePrevSection(occlude, invisible, x, index, z, id, sections)
                                    id++
                                }
                                for (xyz in 0 ..< 16 * 16 * 15)
                                    occlude[id++] = true
                            } else {
                                id += 16 * 16 * 16
                            }
                            i += 16
                            if (i > maxHeightToProceed) break@out
                        }

                        is ListPalette, is MapPalette -> {
                            val isOcclude = BooleanArray(palette.size()) { id -> palette.idToState(id).occlude }
                            val storage = section.chunkData.storage!!
                            forChunk2D { x, z ->
                                if (isOcclude[storage.get(id and 0xfff)])
                                    handleOccludePrevSection(occlude, invisible, x, index, z, id, sections)
                                id++
                            }
                            if (++i > maxHeightToProceed) break@out
                            for (y in 0 ..< 15)
                                forChunk2D { x, z ->
                                    if (isOcclude[storage.get(id and 0xfff)])
                                        handleOcclude(occlude, invisible, x, i, z, id, storage)
                                    id++
                                }
                            if (++i > maxHeightToProceed) break@out
                        }

                        else -> {
                            val storage = section.chunkData.storage!!
                            forChunk2D { x, z ->
                                if (palette.idToState(storage.get(id and 0xfff)).occlude)
                                    handleOccludePrevSection(occlude, invisible, x, index, z, id, sections)
                                id++
                            }
                            if (++i > maxHeightToProceed) break@out
                            for (y in 0 ..< 15)
                                forChunk2D { x, z ->
                                    if (palette.idToState(storage.get(id and 0xfff)).occlude)
                                        handleOcclude(occlude, invisible, x, i, z, id, storage)
                                    id++
                                }
                            if (++i > maxHeightToProceed) break@out
                        }

                    }
                }
                counter.minimalChunks++
                miniChunks[chunkKey] = invisible.clone()
//                println("NO CACHE.... ${System.nanoTime() - tm}")
            }
        }
    }

    private inline fun forChunk2D(crossinline scope: (x: Int, z: Int) -> Unit) {
        for (x in 0 ..< 16)
            for (z in 0 ..< 16)
                scope(x, z)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun handleOccludePrevSection(occlude: BooleanArray, invisible: BooleanArray, x: Int, index: Int, z: Int, id: Int, sections: Array<BaseChunk>) {
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
    private inline fun handleOcclude(occlude: BooleanArray, invisible: BooleanArray, x: Int, i: Int, z: Int, id: Int, storage: BaseStorage) {
        occlude[id] = true
        if (x >= 2 && i >= 2 && z >= 2
            && occlude[id - 0x211] && occlude[id - 0x121] && occlude[id - 0x112]
            && occlude[id - 0x011] && occlude[id - 0x110] && occlude[id - 0x101]) {

            storage.set(id - 0x111 and 0xfff, 0)
            invisible[id - 0x111] = true
        }
    }

    private fun checkBlockUpdate(player: Player, blockLocation: Vector3i, minHeight: Int = player.world.minHeight): Boolean {
        return checkBlockUpdate(player, blockLocation.x, blockLocation.y, blockLocation.z, minHeight)
    }

    private fun checkBlockUpdate(player: Player, x: Int, y: Int, z: Int, minHeight: Int = player.world.minHeight): Boolean {
        val miniChunks = player.miniChunks
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val chunkKey = Chunk.getChunkKey(chunkX, chunkZ)
        val invisible = miniChunks[chunkKey] ?: return true
        if (invisible === FULL_CHUNK) return true

        val id = blockKeyChunkAnd(x, y, z, minHeight)
        return checkBlockUpdate1(player, miniChunks, invisible, chunkX, chunkZ, chunkKey, id)
    }

    private fun checkBlockUpdate(player: Player, chunkX: Int, chunkZ: Int, x: Int, y: Int, z: Int, minHeight: Int = player.world.minHeight): Boolean {
        val miniChunks = player.miniChunks
        val chunkKey = Chunk.getChunkKey(chunkX, chunkZ)
        val invisible = miniChunks[chunkKey] ?: return true
        if (invisible === FULL_CHUNK) return true

        val id = blockKeyChunk(x, y, z, minHeight)
        return checkBlockUpdate1(player, miniChunks, invisible, chunkX, chunkZ, chunkKey, id)
    }

    private fun checkBlockUpdate1(player: Player, miniChunks: Long2ObjectOpenHashMap<BooleanArray>, invisible: BooleanArray, chunkX: Int, chunkZ: Int, chunkKey: Long, blockId: Int): Boolean {
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
                PlayerChunkSender.sendChunk(nms.connection, level, level.`moonrise$getFullChunkIfLoaded`(chunkX, chunkZ))
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
        if (y - minHeight > 0xff) {
            // Overflows
            return -1
        }
        return x or (z shl 4) or (y - minHeight and 0xff shl 8)
    }
    private fun blockKeyChunkAnd(x: Int, y: Int, z: Int, minHeight: Int): Int {
        if (y - minHeight > 0xff) {
            // Overflows
            return -1
        }
        return (x and 0xf) or (z and 0xf shl 4) or (y - minHeight and 0xff shl 8)
    }
    private val Int.chunkBlockPos
        get() = ChunkBlockPos(this and 0xf, this shr 8, this shr 4 and 0xf)
    private data class ChunkBlockPos(val x: Int, val y: Int, val z: Int)
    private inline fun Int.nearbyBlockId(height: Int, crossinline scope: (Int) -> Unit) {
        val (x, y, z) = this.chunkBlockPos
        if (x > 0)  scope(this - 0x001)
        if (x < 16) scope(this + 0x001)
        if (z > 0)  scope(this - 0x010)
        if (z < 16) scope(this + 0x010)
        if (y > 0)  scope(this - 0x100)
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

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
    )
}