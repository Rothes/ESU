package io.github.rothes.esu.bukkit.util.version.adapter.moonrise

import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import io.github.rothes.esu.core.util.extension.ClassUtils
import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

abstract class ChunkLimiterHandler {

    private val gen = RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java.getDeclaredField("chunkGenerateTicketLimiter").usObjAccessor
    private val load = RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java.getDeclaredField("chunkLoadTicketLimiter").usObjAccessor

    abstract fun getAllocationTaken(player: Player, type: Type): Long
    abstract fun previewAllocation(player: Player, type: Type, take: Long): Long
    abstract fun takeAllocation(player: Player, type: Type, take: Long): Long

    open fun getGlobalMaxRate(type: Type): Double {
        val config = GlobalConfiguration.get().chunkLoadingBasic
        return when (type) {
            Type.LOAD       -> config.playerMaxChunkLoadRate
            Type.GENERATE   -> config.playerMaxChunkGenerateRate
            Type.SEND       -> config.playerMaxChunkSendRate
        }
    }

    @Suppress("RedundantNullableReturnType") // TODO: May return null on 1.21.x
    protected val Player.moonriseChunkLoader: RegionizedPlayerChunkLoader.PlayerChunkLoaderData
        get() = (this as CraftPlayer).handle.`moonrise$getChunkLoader`()

    protected fun RegionizedPlayerChunkLoader.PlayerChunkLoaderData.getLimiter(type: Type): Any {
        return when (type) {
            Type.GENERATE   -> gen[this]
            Type.LOAD       -> load[this]
            Type.SEND       -> TODO()
        }
    }

    enum class Type {
        GENERATE,
        LOAD,
        SEND,
    }

    companion object {

        val isSupported
            get() = ClassUtils.existsClass($$"ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader$PlayerChunkLoaderData")

        val instance by lazy { ChunkLimiterHandler::class.java.versioned() }
    }

}
