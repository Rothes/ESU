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
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.networkthrottle.UnknownPacketType
import io.github.rothes.esu.velocity.plugin
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.MessageToByteEncoder
import com.github.retrooper.packetevents.protocol.player.User as PEUser

object Injector {

    private const val ENCODER_NAME_PRE = "esu-encoder-pre"
    private const val ENCODER_NAME_FIN = "esu-encoder-fin"

    private val connectionManager by lazy {
        val server = plugin.server as VelocityServer
        VelocityServer::class.java.declaredFields
            .first { it.type == ConnectionManager::class.java }
            .also { it.isAccessible = true }
            .get(server) as ConnectionManager
    }

    private val encoderHandlers = linkedSetOf<ChannelHandler>()

    fun registerEncoderHandler(channelHandler: ChannelHandler) {
        encoderHandlers.add(channelHandler)
    }

    fun unregisterEncoderHandler(channelHandler: ChannelHandler) {
        encoderHandlers.remove(channelHandler)
    }

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
                inject(channel).player = player
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
        val data = inject(channel)
        data.player = player
    }

    fun inject(channel: Channel): EsuPipelineData {
        return with(channel.pipeline()) {
            if (get(ENCODER_NAME_PRE) != null)
                error("ESU channel handlers are already injected")
            val data = EsuPipelineData(PacketEvents.getAPI().protocolManager.getUser(channel))
            addBefore("minecraft-encoder", ENCODER_NAME_PRE, EsuPreEncoder(data))
            addFirst(ENCODER_NAME_FIN, EsuFinEncoder(data))
            return data
        }
    }

    fun eject(channel: Channel) {
        channel.pipeline().remove(ENCODER_NAME_PRE)
        channel.pipeline().remove(ENCODER_NAME_FIN)
    }

    data class EsuPipelineData(
        val peUser: PEUser,
        var player: Player? = null,
        var packetType: PacketTypeCommon = UnknownPacketType,
        var uncompressedSize: Int = -1,
    )

    class EsuPreEncoder(val data: EsuPipelineData): MessageToByteEncoder<ByteBuf>() {

        override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
            val peUser = data.peUser
            val readerIndex = msg.readerIndex()
            data.uncompressedSize = msg.readableBytes()
            val packetId = ByteBufHelper.readVarInt(msg)
            data.packetType = PacketType.getById(PacketSide.SERVER, peUser.encoderState, peUser.clientVersion, packetId) ?: UnknownPacketType
            msg.readerIndex(readerIndex)

            out.writeBytes(msg)
        }

    }

    class EsuFinEncoder(val data: EsuPipelineData): MessageToByteEncoder<ByteBuf>() {

        override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
            if (encoderHandlers.isNotEmpty()) {
                val packetData = PacketData(data.player, data.packetType, msg, data.uncompressedSize, msg.readableBytes())
                for (handler in encoderHandlers) {
                    try {
                        handler.handle(packetData)
                    } catch (e: Throwable) {
                        plugin.err("Unhandled exception while handling packet", e)
                    }
                }
            }
            out.writeBytes(msg)
        }

    }

    class EsuChannelInitializer(val wrapped: ChannelInitializer<Channel>): ChannelInitializer<Channel>() {

        private val initWrapped = ChannelInitializer::class.java.getDeclaredMethod("initChannel", Channel::class.java).also { it.isAccessible = true }

        override fun initChannel(ch: Channel) {
            initWrapped(wrapped, ch)
            inject(ch)
        }
    }

}