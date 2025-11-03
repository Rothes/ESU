package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.user.UserManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

object BukkitUserManager: UserManager<CommandSender, PlayerUser>() {

    init {
        instance = BukkitUserManager
    }

    override fun get(native: CommandSender): PlayerUser {
        val player = native.asPlayer
        val uuid = player.uniqueId
        return getCache(uuid)?.also { it.playerCache = player } ?: PlayerUser(player).also { set(uuid, it) }
    }

    override fun create(uuid: UUID): PlayerUser = PlayerUser(uuid)

    override fun unload(native: CommandSender): PlayerUser? = unload(native.asPlayer.uniqueId)

    private val CommandSender.asPlayer: Player
        get() = this as Player

}