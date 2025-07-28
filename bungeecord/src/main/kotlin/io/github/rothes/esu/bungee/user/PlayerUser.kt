package io.github.rothes.esu.bungee.user

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.bungee.util.ComponentBungeeUtils.bungee
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.*

class PlayerUser(override val uuid: UUID, initPlayer: ProxiedPlayer? = null): BungeeUser() {

    constructor(player: ProxiedPlayer): this(player.uniqueId, player)

    var playerCache: ProxiedPlayer? = initPlayer
        get() {
            val cache = field
            if (cache != null) {
                return cache
            }
            val get = ProxyServer.getInstance().getPlayer(uuid)
            if (get != null) {
                field = get
                return get
            }
            return field ?: error("Player $uuid is not online and there's no cached instance!")
        }
        internal set
    val player: ProxiedPlayer
        get() = playerCache!!
    override val commandSender: CommandSender
        get() = player
    override val dbId: Int
    override val name: String
        get() = nameUnsafe!!
    override val nameUnsafe: String?
        get() = playerCache?.name
    override val clientLocale: String
        get() = with(player.locale) { language + '_' + country.lowercase() }

    override var languageUnsafe: String?
    override var colorSchemeUnsafe: String?

    override val isOnline: Boolean
        get() = playerCache?.isConnected == true

    init {
        val userData = StorageManager.getUserData(uuid)
        dbId = userData.dbId
        languageUnsafe = userData.language
        colorSchemeUnsafe = userData.colorScheme
    }

    override fun <T : ConfigurationPart> kick(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        player.disconnect(MiniMessage.miniMessage().deserialize(localed(locales, block), *params, colorSchemeInstance).bungee)
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