package io.github.rothes.esu.common

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import io.github.rothes.esu.core.util.extension.*
import io.github.rothes.esu.lib.configurate.yaml.internal.snakeyaml.emitter.Emitter
import io.github.rothes.esu.lib.packetevents.PacketEvents
import io.github.rothes.esu.lib.packetevents.protocol.ConnectionState
import io.github.rothes.esu.lib.packetevents.protocol.player.ClientVersion
import io.github.rothes.esu.lib.packetevents.protocol.player.TextureProperty
import io.github.rothes.esu.lib.packetevents.protocol.player.User
import io.github.rothes.esu.lib.packetevents.protocol.world.dimension.DimensionType
import io.github.rothes.esu.lib.packetevents.protocol.world.dimension.DimensionTypes
import kotlinx.io.*
import org.incendo.cloud.parser.flag.FlagContext
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import java.io.ByteArrayOutputStream
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

private const val TMP_DATA_VERSION = 2

open class HotLoadSupport(
    val isHot: Boolean,
    val hasPacketEventsPlugin: Boolean,
) {

    private val peUserRegistriesGetter = User::class.java.getDeclaredField("registries").usObjAccessor
    private val serverRegistriesGetter by lazy { com.github.retrooper.packetevents.protocol.player.User::class.java.getDeclaredField("registries").usObjAccessor }
    private val dataFile = EsuBootstrap.instance.baseConfigPath().resolve("hot-data.tmp")
    private lateinit var peUserData: MutableMap<UUID, PEUserData>

    fun onEnable() {
        loadCriticalClasses()
        if (isHot) {
            try {
                if (!dataFile.exists()) return
                dataFile.inputStream().asSource().buffered().use { buffer ->
                    require(buffer.readInt() == TMP_DATA_VERSION) { "Different hot data version." }
                    loadPacketEventsData(buffer)
                }
                dataFile.deleteIfExists()
            } catch (e: Throwable) {
                peUserData = mutableMapOf()
                EsuBootstrap.instance.err("Failed to read hot-data", e)
            }
        }
    }

    fun onDisable() {
        if (isHot) {
            try {
                val buffer = Buffer()
                savePacketEventsData(buffer)
                dataFile.outputStream(StandardOpenOption.CREATE).use {
                    buffer.copyTo(it)
                }
                dataFile.toFile().deleteOnExit()
            } catch (e: Throwable) {
                EsuBootstrap.instance.err("Failed to save hot-data", e)
            }
        }
    }

    fun loadPEUser(channel: Any, uuid: UUID, name: String) {
        if (hasPacketEventsPlugin) {
            val user = PacketEvents.getAPI().protocolManager.getUser(channel)
            val server = com.github.retrooper.packetevents.PacketEvents.getAPI().protocolManager.getUser(channel) ?: return
            user.clientVersion = ClientVersion.valueOf(server.clientVersion.name)
            user.decoderState = ConnectionState.valueOf(server.decoderState.name)
            user.encoderState = ConnectionState.valueOf(server.encoderState.name)
            val id = com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypes.getRegistry().getId(server.dimensionType, server.clientVersion)
            user.dimensionType = DimensionTypes.getRegistry().getById(user.clientVersion, id)
//            @Suppress("UNCHECKED_CAST")
//            val ur = peUserRegistriesGetter[user] as Map<ResourceLocation, IRegistry<*>>
//            @Suppress("UNCHECKED_CAST")
//            val sr = serverRegistriesGetter[server] as Map<com.github.retrooper.packetevents.resources.ResourceLocation, com.github.retrooper.packetevents.util.mappings.IRegistry<*>>
            user.entityId = server.entityId
            user.profile.uuid = uuid
            user.profile.name = name
            user.profile.textureProperties = server.profile.textureProperties.map { TextureProperty(it.name, it.value, it.signature) }
            return
        }

        if (!isHot) return // peUserData is not init, don't do anything.
        val data = peUserData[uuid]
        val user = PacketEvents.getAPI().protocolManager.getUser(channel)

        user.profile.uuid = uuid
        user.profile.name = name

        if (data == null) {
            user.connectionState = ConnectionState.PLAY
            user.clientVersion = PacketEvents.getAPI().serverManager.version.toClientVersion()
            EsuBootstrap.instance.warn("No hot packetevents user data for player '$name', this may cause issues.")
            return
        }
        val (_, clientVersion, decoderState, encoderState, dimensionType, entityId, texture) = data
        user.clientVersion = clientVersion
        user.decoderState = decoderState
        user.encoderState = encoderState
        user.dimensionType = dimensionType
        user.entityId = entityId
        user.profile.textureProperties = texture
    }

    private fun loadPacketEventsData(source: Source) {
        peUserData = mutableMapOf()
        with(source) {
            if (readBool()) return // hasPacketEventsPlugin, skipped
            for (i in 0 until readInt()) {
                val user = readUser()
                peUserData[user.uuid] = user
            }
        }
    }

    private fun savePacketEventsData(buffer: Buffer) {
        buffer.writeInt(TMP_DATA_VERSION)
        buffer.writeBool(hasPacketEventsPlugin)
        if (hasPacketEventsPlugin) return
        val protocolManager = PacketEvents.getAPI().protocolManager
        val users = protocolManager.users
        buffer.writeInt(users.size)
        for (user in users) {
            user.uuid ?: return
            buffer.writeUser(PEUserData(user))
        }
    }

    private fun loadCriticalClasses() {
        // Load the classes those are easily to break the hot plugin update.
        Emitter::class.java.declaredClasses // This may cause break when empty data loaded and saving with flow node
        FlagContext::class.java.toString()
        Charsets::class.java.toString()
        UpdateStatement::class.java.toString()
        loadClass("io/github/rothes/esu/common/util/extension/CommandManagersKt")
        loadClass("kotlinx.coroutines.DebugKt") // For JobSupport.nameString(), coroutine exception on shutdown
        loadClass("kotlinx.coroutines.DebugStringsKt") // For JobSupport.nameString(), coroutine exception on shutdown

        // Velocity ServerUtils support
        Buffer().apply {
            writeAscii("Load classes")
            copyTo(ByteArrayOutputStream())
        }
        PEUserData::class.java.toString()
    }

    private fun loadClass(clazz: String) {
        try {
            Class.forName(clazz.replace('/', '.'))
        } catch (e: ClassNotFoundException) {
            EsuBootstrap.instance.warn("HotLoadSupport - Class $clazz not found")
        }
    }

    data class PEUserData(
        val uuid: UUID,
        val clientVersion: ClientVersion,
        val decoderState: ConnectionState,
        val encoderState: ConnectionState,
        val dimensionType: DimensionType,
        val entityId: Int,
        val texture: List<TextureProperty>,
    ) {

        constructor(user: User) : this(
            user.uuid,
            user.clientVersion,
            user.decoderState,
            user.encoderState,
            user.dimensionType,
            user.entityId,
            user.profile.textureProperties
        )

    }

    private fun Sink.writeUser(data: PEUserData) {
        writeUuid(data.uuid)
        writeShortFromInt(data.clientVersion.ordinal)
        writeByteFromInt(data.decoderState.ordinal)
        writeByteFromInt(data.encoderState.ordinal)
        writeInt(DimensionTypes.getRegistry().getId(data.dimensionType, data.clientVersion))
        writeInt(data.entityId)
        writeInt(data.texture.size)
        for (property in data.texture) {
            writeUtf(property.name)
            writeUtf(property.value)
            val signature = property.signature
            writeBool(signature != null)
            if (signature != null) {
                writeAscii(signature)
            }
        }
    }

    private fun Source.readUser(): PEUserData {
        val uuid = readUuid()
        val clientVersion = ClientVersion.entries[readIntFromShort()]
        val decoderState = ConnectionState.entries[readIntFromByte()]
        val encoderState = ConnectionState.entries[readIntFromByte()]
        val dimensionType = DimensionTypes.getRegistry().getById(clientVersion, readInt())!!
        val entityId = readInt()
        val texture = ArrayList<TextureProperty>(readInt())
        for (i in 0 until texture.size) {
            val name = readUtf()
            val value = readUtf()
            val signature = if (readBool()) readAscii() else null
            texture.add(TextureProperty(name, value, signature))
        }
        return PEUserData(uuid, clientVersion, decoderState, encoderState, dimensionType, entityId, texture)
    }

}