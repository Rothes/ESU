package io.github.rothes.esu.common

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.extension.*
import io.github.rothes.esu.lib.configurate.yaml.internal.snakeyaml.emitter.Emitter
import io.github.rothes.esu.lib.packetevents.PacketEvents
import io.github.rothes.esu.lib.packetevents.protocol.ConnectionState
import io.github.rothes.esu.lib.packetevents.protocol.player.ClientVersion
import io.github.rothes.esu.lib.packetevents.protocol.player.TextureProperty
import io.github.rothes.esu.lib.packetevents.protocol.player.User
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

open class HotLoadSupport(
    val isHot: Boolean,
    val hasPacketEventsPlugin: Boolean,
) {

    private val dataFile = EsuBootstrap.instance.baseConfigPath().resolve("hot-data.tmp")
    private lateinit var peUserData: MutableMap<UUID, PEUserData>

    fun onEnable() {
        loadCriticalClasses()
        if (isHot) {
            try {
                if (!dataFile.exists()) return
                dataFile.inputStream().asSource().buffered().use {
                    loadPacketEventsData(it)
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
        val (_, clientVersion, decoderState, encoderState, entityId, texture) = data
        user.clientVersion = clientVersion
        user.decoderState = decoderState
        user.encoderState = encoderState
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

        // Velocity ServerUtils support
        Buffer().apply {
            writeAscii("Load classes")
            copyTo(ByteArrayOutputStream())
        }
        PEUserData::class.java.toString()
    }

    data class PEUserData(
        val uuid: UUID,
        val clientVersion: ClientVersion,
        val decoderState: ConnectionState,
        val encoderState: ConnectionState,
        val entityId: Int,
        val texture: List<TextureProperty>,
    ) {

        constructor(user: User) : this(
            user.uuid,
            user.clientVersion,
            user.decoderState,
            user.encoderState,
            user.entityId,
            user.profile.textureProperties
        )

    }

    private fun Sink.writeUser(data: PEUserData) {
        writeUuid(data.uuid)
        writeShortFromInt(data.clientVersion.ordinal)
        writeByteFromInt(data.decoderState.ordinal)
        writeByteFromInt(data.encoderState.ordinal)
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
        val entityId = readInt()
        val texture = ArrayList<TextureProperty>(readInt())
        for (i in 0 until texture.size) {
            val name = readUtf()
            val value = readUtf()
            val signature = if (readBool()) readAscii() else null
            texture.add(TextureProperty(name, value, signature))
        }
        return PEUserData(uuid, clientVersion, decoderState, encoderState, entityId, texture)
    }

}