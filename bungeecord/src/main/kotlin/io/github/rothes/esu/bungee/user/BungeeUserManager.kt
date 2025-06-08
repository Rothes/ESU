package io.github.rothes.esu.bungee.user

import io.github.rothes.esu.core.user.UserManager
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.*

object BungeeUserManager: UserManager<ProxiedPlayer, PlayerUser>() {

    init {
        instance = BungeeUserManager
    }

    override fun get(native: ProxiedPlayer): PlayerUser {
        val uuid = native.uniqueId
        return byUuid[uuid]?.also { it.playerCache = native } ?: PlayerUser(native).also { byUuid[uuid] = it }
    }

    override fun create(uuid: UUID): PlayerUser = PlayerUser(uuid)

    override fun unload(native: ProxiedPlayer): PlayerUser? = unload(native.uniqueId)

}