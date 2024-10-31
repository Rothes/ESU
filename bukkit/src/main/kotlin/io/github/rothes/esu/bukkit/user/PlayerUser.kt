package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.storage.StorageManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class PlayerUser(override val uuid: UUID, initPlayer: Player? = null): BukkitUser() {

    constructor(player: Player): this(player.uniqueId, player)

    var playerCache: Player? = initPlayer
        get() = field ?: Bukkit.getPlayer(uuid)?.also { field = it }
            ?: throw IllegalStateException("Player $uuid is not online and there's no cached instance!")
        internal set
    val player: Player
        get() = playerCache!!
    override val commandSender: CommandSender
        get() = player
    override val dbId: Int = StorageManager.getUserId(uuid)
    override val nameUnsafe: String?
        get() = playerCache?.name
    override val clientLocale: String
        get() = with(player.locale()) { language + '_' + country.lowercase() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerUser

        if (dbId != other.dbId) return false
        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + dbId
        return result
    }

}