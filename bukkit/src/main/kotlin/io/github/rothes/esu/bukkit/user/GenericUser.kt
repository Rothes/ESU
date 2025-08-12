package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import java.util.*

class GenericUser(override val commandSender: CommandSender): BukkitUser() {

    override val dbId: Int
        get() = throw UnsupportedOperationException()
    override val uuid: UUID
        get() = throw UnsupportedOperationException()
    override val nameUnsafe: String
        get() = name
    override val clientLocale: String
        get() = EsuConfig.get().locale

    override var languageUnsafe: String? = null
    override var colorSchemeUnsafe: String? = null

    override val isOnline: Boolean = false

    override fun <T : ConfigurationPart> kick(locales: MultiLocaleConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        throw UnsupportedOperationException("Cannot kick a GenericUser")
    }

}