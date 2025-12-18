package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

val plugin: EsuPluginBukkit
    get() = EsuCore.instance as EsuPluginBukkit

val bootstrap: EsuBootstrapBukkit
    get() = EsuBootstrap.instance as EsuBootstrapBukkit

val adventure = BukkitAudiences.create(bootstrap)

val Player.user: PlayerUser
    get() = BukkitUserManager[this]
val UUID.playerUser: PlayerUser
    get() = BukkitUserManager[this]

val CommandSender.audience: Audience
    get() = adventure.sender(this)