package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.AdventureHolder.adventure
import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.util.ComponentUtils
import io.github.rothes.esu.lib.net.kyori.adventure.audience.Audience
import io.github.rothes.esu.lib.net.kyori.adventure.platform.bukkit.BukkitAudiences
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

val plugin: EsuPluginBukkit
    get() = EsuCore.instance as EsuPluginBukkit

val bootstrap: EsuBootstrapBukkit
    get() = EsuBootstrap.instance as EsuBootstrapBukkit

val String.legacy: Component
    get() = ComponentUtils.fromLegacy(this)

val Player.user: PlayerUser
    get() = BukkitUserManager[this]
val UUID.playerUser: PlayerUser
    get() = BukkitUserManager[this]

val CommandSender.audience: Audience
    get() = adventure.sender(this)

object AdventureHolder {
    // Wrap with an object, to avoid init error before we load dependencies
    val adventure = BukkitAudiences.create(bootstrap)
}