package io.github.rothes.esu.velocity.module.serverinfo

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerPluginMessage
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage
import com.velocitypowered.proxy.protocol.ProtocolUtils
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.velocity.util.extension.checkPacketEvents
import io.netty.buffer.Unpooled
import java.nio.charset.StandardCharsets

object RebrandFeature : CommonFeature<RebrandFeature.FeatureConfig, Unit>() {

    private const val BRAND_CHANNEL = "minecraft:brand"
    private const val BRAND_CHANNEL_LEGACY = "MC|Brand"

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents()
    }

    override fun onEnable() {
        PacketEvents.getAPI().eventManager.registerListener(PacketListeners)
    }

    override fun onDisable() {
        super.onDisable()
        PacketEvents.getAPI().eventManager.unregisterListener(PacketListeners)
    }

    private object PacketListeners : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketSend(event: PacketSendEvent) {
            when (event.packetType) {
                PacketType.Configuration.Server.PLUGIN_MESSAGE -> {
                    val wrapper = WrapperConfigServerPluginMessage(event)
                    if (isMcBrand(wrapper.channelName)) {
                        val serverBrand = getServerBrand(readBrand(wrapper.data))
                        wrapper.data = createBrand(event.user.clientVersion, serverBrand)
                    }
                }
                PacketType.Play.Server.PLUGIN_MESSAGE          -> {
                    val wrapper = WrapperPlayServerPluginMessage(event)
                    if (isMcBrand(wrapper.channelName)) {
                        val serverBrand = getServerBrand(readBrand(wrapper.data))
                        wrapper.data = createBrand(event.user.clientVersion, serverBrand)
                    }
                }
            }
        }

        private fun isMcBrand(channelName: String): Boolean {
            return channelName == BRAND_CHANNEL || channelName == BRAND_CHANNEL_LEGACY
        }

        private fun readBrand(data: ByteArray): String {
            return PluginMessageUtil.readBrandMessage(Unpooled.wrappedBuffer(data))
        }

        private fun getServerBrand(brand: String): String {
            // BackendServerBrand (VelocityBrand)
            // We only need 'BackendServerBrand' part
            return brand.substringBeforeLast(' ')
        }

        private fun createBrand(protocolVersion: ClientVersion, serverBrand: String): ByteArray {
            val brand = config.brandOverride.replace("<server-brand>", serverBrand)
            val buf = Unpooled.buffer()
            if (protocolVersion >= ClientVersion.V_1_8) {
                ProtocolUtils.writeString(buf, brand)
            } else {
                buf.writeCharSequence(brand, StandardCharsets.UTF_8)
            }
            val array = ByteArray(buf.readableBytes())
            buf.readBytes(array)
            return array
        }

    }

    data class FeatureConfig(
        val brandOverride: String = "<server-brand> §7-§r Rebrand"
    ) : BaseFeatureConfiguration()

}