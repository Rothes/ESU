package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.EsuConfig
import io.github.rothes.esu.core.user.ConsoleConst
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.util.*

object ConsoleUser: BukkitUser() {

    override val commandSender: CommandSender = Bukkit.getConsoleSender()
    override val dbId: Int = ConsoleConst.DATABASE_ID
    override val name: String = ConsoleConst.NAME
    override val nameUnsafe: String = name
    override val clientLocale: String
        get() = EsuConfig.get().locale
    override val uuid: UUID = ConsoleConst.UUID

}