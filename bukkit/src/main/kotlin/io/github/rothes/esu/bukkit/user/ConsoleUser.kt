package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.user.ConsoleConst
import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
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

    init {
        val userData = StorageManager.getConsoleUserData()
        dbId = userData.dbId
        languageUnsafe = userData.language
        colorSchemeUnsafe = userData.colorScheme
    }

    override fun <T> kick(locales: MultiLangConfiguration<T>, block: T.() -> String?, vararg params: TagResolver) {
        throw UnsupportedOperationException("Cannot kick a ConsoleUser")
    }

    override fun print(string: String) {
        commandSender.sendMessage(string)
    }

}