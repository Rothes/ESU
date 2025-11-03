package io.github.rothes.esu.velocity.user

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import io.github.rothes.esu.core.user.UserManager
import java.util.*

object VelocityUserManager: UserManager<CommandSource, PlayerUser>() {

    init {
        instance = VelocityUserManager
    }

    override fun get(native: CommandSource): PlayerUser {
        val player = native.asPlayer
        val uuid = player.uniqueId
        return getCache(uuid)?.also { it.playerCache = player } ?: PlayerUser(player).also { set(uuid, it) }
    }

    override fun create(uuid: UUID): PlayerUser = PlayerUser(uuid)

    override fun unload(native: CommandSource): PlayerUser? = unload(native.asPlayer.uniqueId)

    private val CommandSource.asPlayer: Player
        get() = this as Player

}