package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.*

val plugin: Plugin
    get() = EsuBootstrap.instance as Plugin

val core: EsuCoreBukkit
    get() = EsuCore.instance as EsuCoreBukkit

val adventure = BukkitAudiences.create(plugin)

val Player.user: PlayerUser
    get() = BukkitUserManager[this]
val UUID.playerUser: PlayerUser
    get() = BukkitUserManager[this]

val CommandSender.audience: Audience
    get() = adventure.sender(this)