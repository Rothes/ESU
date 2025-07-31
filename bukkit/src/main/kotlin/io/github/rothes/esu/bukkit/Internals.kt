package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.ServerCompatibility.CB.adventure
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.util.ComponentUtils
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

val plugin: EsuPluginBukkit
    get() = EsuCore.instance as EsuPluginBukkit

val String.legacy: Component
    get() = ComponentUtils.fromLegacy(this)

val Player.user: PlayerUser
    get() = BukkitUserManager[this]
val UUID.playerUser: PlayerUser
    get() = BukkitUserManager[this]

val CommandSender.audience: Audience
    get() = if (ServerCompatibility.paper) this else adventure.sender(this)