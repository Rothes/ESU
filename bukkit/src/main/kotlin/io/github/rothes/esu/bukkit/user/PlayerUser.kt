package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.audience
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.connected
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.text.minimessage.MiniMessage
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class PlayerUser(override val uuid: UUID, initPlayer: Player? = null): BukkitUser() {

    constructor(player: Player): this(player.uniqueId, player)

    override val audience: Audience
        get() = player.audience

    var playerCache: Player? = initPlayer
        get() {
            val cache = field
            if (cache != null) {
                // Check if the instance is as it is.
                if (cache.isOnline && cache.connected) {
                    return cache
                }
            }
            val get = Bukkit.getPlayer(uuid)
            if (get != null) {
                field = get
                return get
            }
            return cache
        }
        internal set
    val player: Player
        get() = playerCache ?: error("Player $uuid is not online and there's no cached instance!")
    override val commandSender: CommandSender
        get() = player
    override val dbId: Int
    override val nameUnsafe: String?
        get() = playerCache?.name
    override val clientLocale: String
        get() = player.locale

    override var languageUnsafe: String?
    override var colorSchemeUnsafe: String?

    override val isOnline: Boolean
        get() = playerCache?.isOnline == true
    var logonBefore: Boolean = false
        internal set

    init {
        val userData = StorageManager.getUserData(uuid)
        dbId = userData.dbId
        languageUnsafe = userData.language
        colorSchemeUnsafe = userData.colorScheme
    }

    override fun <T> kick(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        player.kick(MiniMessage.miniMessage().deserialize(localed(locales, block), *params,
            ColorSchemes.schemes.get(colorScheme) { tagResolver }!!).server)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerUser

        if (dbId != other.dbId) return false
        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dbId
        result = 31 * result + uuid.hashCode()
        return result
    }

}