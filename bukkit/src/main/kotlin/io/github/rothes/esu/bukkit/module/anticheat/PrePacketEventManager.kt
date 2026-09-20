package io.github.rothes.esu.bukkit.module.anticheat

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.EventManager
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.exception.PacketProcessException
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.github.retrooper.packetevents.protocol.ConnectionState
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.util.EventCreationUtil
import com.github.retrooper.packetevents.util.ExceptionUtil
import io.github.retrooper.packetevents.injector.handlers.PacketEventsDecoder
import io.github.rothes.esu.bukkit.util.ServerInfo
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext

/*
 * Hold a decoder that should appear before other anticheat plugins, and before via
 */
object PrePacketEventManager {

    private const val PRE_ESU_AC_DECODER_NAME = "pre-pe-decoder-esu-ac"

    private const val PE_DECODER_NAME = "pe-decoder-packetevents"
    private const val GRIM_PRE_DECODER_NAME = "pre-pe-decoder-grimac"

    private const val VIA_DECODER_NAME = "via-decoder"

    val eventManager = EventManager()

    private var injected = false

    internal fun inject() {
        if (injected) return

        PacketEvents.getAPI().eventManager.registerListener(InjectListener)
        for (channel in PacketEvents.getAPI().protocolManager.channels) {
            injectChannel(channel as Channel)
        }
        injected = true
    }

    internal fun eject() {
        if (!injected || !ServerInfo.PluginEnabled.PacketEvents) return

        PacketEvents.getAPI().eventManager.unregisterListener(InjectListener)
        for (channel in PacketEvents.getAPI().protocolManager.channels) {
            ejectChannel(channel as Channel)
        }
        injected = false
    }

    private fun injectChannel(channel: Channel) {
        val pipeline = channel.pipeline()

        val peDecoder = pipeline.get(PE_DECODER_NAME) as PacketEventsDecoder
        val esuPreDecoder = EsuPreDecoder(peDecoder)
        if (pipeline.get(GRIM_PRE_DECODER_NAME) != null) {
            pipeline.addBefore(GRIM_PRE_DECODER_NAME, PRE_ESU_AC_DECODER_NAME, esuPreDecoder)
        } else if (pipeline.get(VIA_DECODER_NAME) != null) {
            pipeline.addBefore(VIA_DECODER_NAME, PRE_ESU_AC_DECODER_NAME, esuPreDecoder)
        } else {
            pipeline.addBefore(PE_DECODER_NAME, PRE_ESU_AC_DECODER_NAME, esuPreDecoder)
        }
    }

    private fun ejectChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        if (pipeline.get(PRE_ESU_AC_DECODER_NAME) != null) pipeline.remove(PRE_ESU_AC_DECODER_NAME)
    }

    private object InjectListener : PacketListenerAbstract(PacketListenerPriority.LOWEST) {

        override fun onPacketSend(event: PacketSendEvent) {
            if (event.packetType == PacketType.Login.Server.LOGIN_SUCCESS) {
                // Inject here, SET_COMPRESSION is right before LOGIN_SUCCESS
                injectChannel(event.channel as Channel)
            }
        }

    }

    private class EsuPreDecoder(private val decoder: PacketEventsDecoder) : PacketEventsDecoder(decoder) {

        init {
            user = VersionUser(user) // Override getPacketVersion
        }

        override fun read(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any?>) {
            try {
                handlePacket(ctx, input)
                out.add(ByteBufHelper.retain(input))
            } catch (e: Throwable) {
                if (ExceptionUtil.isException(e, PacketProcessException::class.java)) {
                    throw e
                } else {
                    throw PacketProcessException(e)
                }
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
            player = decoder.player // Sync player
            super.exceptionCaught(ctx, cause)
        }

        // com.github.retrooper.packetevents.util.PacketEventsImplHelper.handleServerBoundPacket
        // Only change eventManager and decoder.player
        private fun handlePacket(ctx: ChannelHandlerContext, buffer: ByteBuf) {
            if (!ByteBufHelper.isReadable(buffer)) return

            val preProcessIndex = ByteBufHelper.readerIndex(buffer)
            val packetReceiveEvent = EventCreationUtil.createReceiveEvent(ctx.channel(), user, decoder.player, buffer, false)
            val processIndex = ByteBufHelper.readerIndex(buffer)
            eventManager.callEvent(packetReceiveEvent) {
                ByteBufHelper.readerIndex(buffer, processIndex)
            }
            if (!packetReceiveEvent.isCancelled) {
                //Did they ever use a wrapper?
                val wrapper = packetReceiveEvent.lastUsedWrapper
                if (wrapper != null) {
                    //Rewrite the buffer
                    ByteBufHelper.clear(buffer)
                    wrapper.writeVarInt(packetReceiveEvent.packetId)
                    wrapper.write()
                } else {
                    //If no wrappers were used, just pass on the original buffer.
                    //Correct the reader index, basically what the next handler is expecting.
                    ByteBufHelper.readerIndex(buffer, preProcessIndex)
                }
            } else {
                //Cancelling the packet, lets clear the buffer
                ByteBufHelper.clear(buffer)
            }
            if (packetReceiveEvent.hasPostTasks()) {
                for (task in packetReceiveEvent.getPostTasks()) {
                    task.run()
                }
            }
        }

    }

    private class VersionUser(private val parent: User) : User(parent.channel, parent.decoderState, parent.clientVersion, parent.profile) {

        override fun getConnectionState(): ConnectionState? = parent.connectionState
        override fun getDecoderState(): ConnectionState? = parent.decoderState
        override fun getEncoderState(): ConnectionState? = parent.encoderState
        override fun getClientVersion(): ClientVersion? = parent.clientVersion
        override fun getProfile(): UserProfile? = parent.profile
        override fun getEntityId(): Int = parent.entityId

        override fun getPacketVersion(): ClientVersion? = clientVersion

    }

}