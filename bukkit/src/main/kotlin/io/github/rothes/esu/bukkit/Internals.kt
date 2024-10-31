package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.EsuCore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import java.util.UUID

val plugin: EsuPluginBukkit
    get() = EsuCore.instance as EsuPluginBukkit

val String.legacy: Component
    get() = LegacyComponentSerializer.legacySection().deserialize(this)

val Player.user: PlayerUser
    get() = BukkitUserManager[this]
val UUID.playerUser: PlayerUser
    get() = BukkitUserManager[this]