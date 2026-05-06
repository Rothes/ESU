package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.user.ConsoleConst
import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.slf4j.Logger
import java.util.*

object ConsoleUser: BukkitUser(), LogUser {

    override val commandSender: CommandSender = Bukkit.getConsoleSender()
    override val dbId: Int
    override val name: String = ConsoleConst.NAME
    override val nameUnsafe: String = name
    override val clientLocale: String
        get() = EsuConfig.get().locale
    override val uuid: UUID = ConsoleConst.UUID

    override var languageUnsafe: String?
    override var colorSchemeUnsafe: String?

    override val isOnline: Boolean = true

    override val logger: Logger = org.slf4j.LoggerFactory.getLogger(plugin.logger.name)

    init {
        val userData = StorageManager.getConsoleUserData()
        dbId = userData.dbId
        languageUnsafe = userData.language
        colorSchemeUnsafe = userData.colorScheme

        LogUser.console = this
    }

    override fun <T> kick(lang: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        throw UnsupportedOperationException("Cannot kick a ConsoleUser")
    }

    override fun print(message: String) {
        commandSender.sendMessage(message)
    }

}