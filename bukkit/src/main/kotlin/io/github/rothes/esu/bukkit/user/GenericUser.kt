package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.EsuConfig
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

}