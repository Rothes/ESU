package io.github.rothes.esu.velocity.user

import com.velocitypowered.api.command.CommandSource
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.user.ConsoleConst
import io.github.rothes.esu.core.user.LogUser
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.velocity.plugin
import java.util.*

object ConsoleUser: VelocityUser(), LogUser {

    override val commandSender: CommandSource = plugin.server.consoleCommandSource
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
        commandSender.sendPlainMessage(string)
    }

}