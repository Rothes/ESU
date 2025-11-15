package io.github.rothes.esu.velocity.module.networkthrottle.channel

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.github.retrooper.packetevents.protocol.PacketSide
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.proxy.VelocityServer
import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import com.velocitypowered.proxy.network.ConnectionManager
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.networkthrottle.UnknownPacketType
import io.github.rothes.esu.velocity.plugin
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import com.github.retrooper.packetevents.protocol.player.User as PEUser

object Injector {

    private const val ENCODER_NAME_PRE = "esu-encoder-pre"
    private const val ENCODER_NAME_FIN = "esu-encoder-fin"
    private const val DECODER_NAME_PRE = "esu-decoder-pre"
    private const val DECODER_NAME_FIN = "esu-decoder-fin"

    val connectionManager by lazy {
        val server = plugin.server as VelocityServer
        VelocityServer::class.java.declaredFields
            .first { it.type == ConnectionManager::class.java }
            .also { it.isAccessible = true }
            .get(server) as ConnectionManager
    }

    private val encoderHandlers = linkedSetOf<EncoderChannelHandler>()
    private val decoderHandlers = linkedSetOf<DecoderChannelHandler>()

    fun registerHandler(channelHandler: EncoderChannelHandler) = encoderHandlers.add(channelHandler)
    fun unregisterHandler(channelHandler: EncoderChannelHandler) = encoderHandlers.remove(channelHandler)
    fun registerHandler(channelHandler: DecoderChannelHandler) = decoderHandlers.add(channelHandler)
    fun unregisterHandler(channelHandler: DecoderChannelHandler) = decoderHandlers.remove(channelHandler)

    fun enable() {
        NetworkThrottleModule.registerListener(this)
        val channelInitializer = connectionManager.serverChannelInitializer.get()
        connectionManager.serverChannelInitializer.set(EsuChannelInitializer(channelInitializer))
        // If velocity is already running, we need to rebind to apply the changes.
        if (plugin.initialized || plugin.enabledHot) {
            connectionManager.close(plugin.server.boundAddress)
            connectionManager.bind(plugin.server.boundAddress)
        }
        for (player in plugin.server.allPlayers) {
            val channel = (player as ConnectedPlayer).connection.channel
            try {
                for (data in inject(channel)) {
                    data.player = player
                }
            } catch (e: IllegalStateException) {
                plugin.err("Failed to inject for player ${player.username} at startup", e)
            }
        }
    }

    fun disable() {
        val channelInitializer = connectionManager.serverChannelInitializer.get()
        if (channelInitializer is EsuChannelInitializer) {
            connectionManager.serverChannelInitializer.set(channelInitializer.wrapped)
            if (plugin.enabled || plugin.disabledHot) {
                connectionManager.close(plugin.server.boundAddress)
                connectionManager.bind(plugin.server.boundAddress)
            }
        } else {
            plugin.warn("Cannot restore ServerChannelInitializerHolder; Value is " + channelInitializer.javaClass.canonicalName)
        }
        for (player in plugin.server.allPlayers) {
            val channel = (player as ConnectedPlayer).connection.channel
            eject(channel)
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onPostLogin(e: PostLoginEvent) {
        val player = e.player as ConnectedPlayer
        val channel = player.connection.channel ?: return
        // Re-inject, because velocity add compression-encoder at this period, and we may not get packet type property.
        eject(channel)
        for (data in inject(channel)) {
            data.player = player
        }
    }

    fun inject(channel: Channel): List<EsuPipelineData> {
        return with(channel.pipeline()) {
            if (get(ENCODER_NAME_PRE) != null)
                error("ESU channel handlers are already injected")
            val outgoing = EsuPipelineData(PacketEvents.getAPI().protocolManager.getUser(channel))
            addBefore("minecraft-encoder", ENCODER_NAME_PRE, EsuPreEncoder(outgoing))
            addFirst(ENCODER_NAME_FIN, EsuFinEncoder(outgoing))

            val incoming = EsuPipelineData(PacketEvents.getAPI().protocolManager.getUser(channel))
            addFirst(DECODER_NAME_PRE, EsuPreDecoder(incoming))
            addBefore("minecraft-decoder", DECODER_NAME_FIN, EsuFinDecoder(incoming))
            listOf(incoming, outgoing)
        }
    }

    fun eject(channel: Channel) {
        channel.pipeline().remove(ENCODER_NAME_PRE)
        channel.pipeline().remove(ENCODER_NAME_FIN)
        channel.pipeline().remove(DECODER_NAME_PRE)
        channel.pipeline().remove(DECODER_NAME_FIN)
    }

    data class EsuPipelineData(
        val peUser: PEUser,
        var player: Player? = null,
        var packetType: PacketTypeCommon = UnknownPacketType,
        /** Stores uncompressed size on encoder, and compressed size on decoder. */
        var oppositeSize: Int = -1,
    )

    class EsuPreEncoder(val data: EsuPipelineData): ChannelOutboundHandlerAdapter() {

        override fun write(ctx: ChannelHandlerContext, msg: Any?, promise: ChannelPromise?) {
            if (msg is ByteBuf) {
                val peUser = data.peUser
                val readerIndex = msg.readerIndex()
                val packetId = ByteBufHelper.readVarInt(msg)
                msg.readerIndex(readerIndex)
                data.oppositeSize = msg.readableBytes()
                data.packetType = PacketType.getById(PacketSide.SERVER, peUser.encoderState, peUser.clientVersion, packetId) ?: UnknownPacketType
            }
            ctx.write(msg, promise)
        }

    }

    class EsuFinEncoder(val data: EsuPipelineData): ChannelOutboundHandlerAdapter() {

        override fun write(ctx: ChannelHandlerContext, msg: Any?, promise: ChannelPromise?) {
            if (encoderHandlers.isNotEmpty() && msg is ByteBuf) {
                val packetData = PacketData(data.player, data.packetType, data.oppositeSize, msg.readableBytes())
                for (handler in encoderHandlers) {
                    try {
                        handler.encode(packetData)
                    } catch (e: Throwable) {
                        plugin.err("Unhandled exception while handling packet", e)
                    }
                }
            }
            ctx.write(msg, promise)
        }

        override fun flush(ctx: ChannelHandlerContext) {
            if (encoderHandlers.isNotEmpty()) {
                for (handler in encoderHandlers) {
                    try {
                        handler.flush()
                    } catch (e: Throwable) {
                        plugin.err("Unhandled exception while handling packet", e)
                    }
                }
            }
            ctx.flush()
        }

    }

    class EsuPreDecoder(val data: EsuPipelineData): ChannelInboundHandlerAdapter() {

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
            if (msg is ByteBuf) {
                data.oppositeSize = msg.readableBytes()
            } else {
                data.oppositeSize = -1
            }
            ctx.fireChannelRead(msg)
        }

    }

    class EsuFinDecoder(val data: EsuPipelineData): ChannelInboundHandlerAdapter() {

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
            if (msg is ByteBuf) {
                val peUser = data.peUser
                val readerIndex = msg.readerIndex()
                val packetId = ByteBufHelper.readVarInt(msg)
                msg.readerIndex(readerIndex)
                val packetType = PacketType.getById(PacketSide.CLIENT, peUser.encoderState, peUser.clientVersion, packetId) ?: UnknownPacketType
                val packetData = PacketData(data.player, packetType, data.oppositeSize, msg.readableBytes())
                for (handler in decoderHandlers) {
                    handler.decode(packetData)
                }
            }
            ctx.fireChannelRead(msg)
        }

    }

    class EsuChannelInitializer(val wrapped: ChannelInitializer<Channel>): ChannelInitializer<Channel>() {

        private val initWrapped = ChannelInitializer::class.java.getDeclaredMethod("initChannel", Channel::class.java).handle

        override fun initChannel(ch: Channel) {
            initWrapped(wrapped, ch)
            if (plugin.enabled)
                inject(ch)
        }
    }

}