package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import org.bukkit.Bukkit

object CommonActions {

    val CHAT = ArgumentAction.create("Chat") { user, arg ->
        arg ?: return@create
        (user as PlayerUser).player.chat(arg)
    }
    val CLOSE = SimpleAction.create("Close") { user ->
        (user as? PlayerUser)?.player?.closeInventory()
    }
    val COMMAND = ArgumentAction.create("Command") { user, arg ->
        arg ?: return@create
        Bukkit.dispatchCommand((user as BukkitUser).commandSender, arg)
    }
    val MESSAGE = ArgumentAction.create("Message") { user, arg ->
        arg ?: return@create
        user.message(arg.message)
    }

}