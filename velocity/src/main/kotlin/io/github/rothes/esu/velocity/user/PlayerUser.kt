package io.github.rothes.esu.velocity.user

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.velocity.plugin
import java.util.*
import kotlin.jvm.optionals.getOrNull

class PlayerUser(override val uuid: UUID, initPlayer: Player? = null): VelocityUser() {

    constructor(player: Player): this(player.uniqueId, player)

    var playerCache: Player? = initPlayer
        get() {
            val cache = field
            if (cache != null) {
                // Check if the instance is as it is.
                if (cache.isActive) {
                    return cache
                }
            }
            val get = plugin.server.getPlayer(uuid).getOrNull()
            if (get != null) {
                field = get
                return get
            }
            return cache
        }
        internal set
    val player: Player
        get() = playerCache ?: error("Player $uuid is not online and there's no cached instance!")
    override val commandSender: CommandSource
        get() = player
    override val dbId: Int
    override val name: String
        get() = nameUnsafe!!
    override val nameUnsafe: String?
        get() = playerCache?.username
    override val clientLocale: String
        get() = with(player.playerSettings.locale) { language + '_' + country.lowercase() }

    override var languageUnsafe: String?
    override var colorSchemeUnsafe: String?

    override val isOnline: Boolean
        get() = playerCache?.isActive == true

    init {
        val userData = StorageManager.getUserData(uuid)
        dbId = userData.dbId
        languageUnsafe = userData.language
        colorSchemeUnsafe = userData.colorScheme
    }

    override fun <T> kick(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        player.disconnect(buildMiniMessage(locales, block, *params).server)
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