package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class PlayerUser(override val uuid: UUID, initPlayer: Player? = null): BukkitUser() {

    constructor(player: Player): this(player.uniqueId, player)

    var playerCache: Player? = initPlayer
        get() {
            val cache = field
            if (cache != null) {
                // Check if the instance is as it is.
                if (cache.isOnline && cache.isConnected) {
                    return cache
                }
            }
            val get = Bukkit.getPlayer(uuid)
            if (get != null) {
                field = get
                return get
            }
            return cache ?: error("Player $uuid is not online and there's no cached instance!")
        }
        internal set
    val player: Player
        get() = playerCache!!
    override val commandSender: CommandSender
        get() = player
    override val dbId: Int
    override val nameUnsafe: String?
        get() = playerCache?.name
    override val clientLocale: String
        get() = with(player.locale()) { language + '_' + country.lowercase() }

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

    override fun <T : ConfigurationPart> kick(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        player.kick(MiniMessage.miniMessage().deserialize(localed(locales, block), *params,
            ColorSchemes.schemes.get(colorScheme) { tagResolver }!!))
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