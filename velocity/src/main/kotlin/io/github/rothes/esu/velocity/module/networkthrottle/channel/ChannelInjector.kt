package io.github.rothes.esu.velocity.module.networkthrottle.channel

import com.velocitypowered.api.proxy.Player
import io.netty.channel.Channel

interface ChannelInjector {

    fun reinjectOnConfiguration(): Boolean = false

    fun inject(channel: Channel, player: Player?)
    fun uninject(channel: Channel, player: Player?)

}