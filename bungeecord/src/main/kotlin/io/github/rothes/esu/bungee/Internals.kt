package io.github.rothes.esu.bungee

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.bungee.user.PlayerUser
import io.github.rothes.esu.bungee.user.BungeeUserManager
import io.github.rothes.esu.lib.net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.*

val plugin: EsuPluginBungee
    get() = EsuCore.instance as EsuPluginBungee
val adventure: BungeeAudiences
    get() = plugin.adventure ?: error("No adventure")

val ProxiedPlayer.user: PlayerUser
    get() = BungeeUserManager[this]
val UUID.playerUser: PlayerUser
    get() = BungeeUserManager[this]